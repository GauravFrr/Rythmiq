package com.premium.spotifyclone.data.repository

import com.premium.spotifyclone.data.local.AppDatabase
import com.premium.spotifyclone.data.local.LikedTrackEntity
import com.premium.spotifyclone.data.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LikedTracksRepository(database: AppDatabase) {

    private val dao = database.likedTrackDao()
    private val writeMutex = Mutex()

    fun observeLikedTracks(): Flow<List<Track>> =
        dao.observeAll().map { list -> list.map { it.toTrack() } }

    fun observeIsLiked(trackId: String): Flow<Boolean> {
        if (trackId.isEmpty()) return flowOf(false)
        return dao.observeLikeCount(trackId)
            .map { it > 0 }
            .distinctUntilChanged()
    }

    suspend fun setLiked(track: Track, liked: Boolean) {
        val url = track.audioUrl ?: return
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                if (liked) {
                    dao.insert(
                        LikedTrackEntity(
                            id = track.id,
                            title = track.title,
                            artist = track.artist,
                            coverUrl = track.coverUrl,
                            durationMs = track.durationMs,
                            audioUrl = url
                        )
                    )
                } else {
                    dao.deleteById(track.id)
                }
            }
        }
    }

    suspend fun toggle(track: Track): Boolean {
        val url = track.audioUrl ?: return false
        return withContext(Dispatchers.IO) {
            writeMutex.withLock {
                dao.toggleLike(
                    LikedTrackEntity(
                        id = track.id,
                        title = track.title,
                        artist = track.artist,
                        coverUrl = track.coverUrl,
                        durationMs = track.durationMs,
                        audioUrl = url
                    )
                )
            }
        }
    }

    private fun LikedTrackEntity.toTrack() = Track(
        id = id,
        title = title,
        artist = artist,
        coverUrl = coverUrl,
        durationMs = durationMs,
        audioUrl = audioUrl
    )
}
