package com.premium.spotifyclone.data.repository

import com.premium.spotifyclone.data.network.SpotifyApiService
import com.premium.spotifyclone.data.network.StreamResponse
import com.premium.spotifyclone.data.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val apiService: SpotifyApiService) {

    suspend fun getTrendingTracks(limit: Int = 20): List<Track> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getRecentlyPlayed()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getPersonalizedFeed(userId: String? = null): List<Track> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getMadeForYou(userId = userId, limit = 15)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getNextRecommendations(currentSongId: String, excludeIds: List<String>, limit: Int = 10): List<Track> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getNextRecommendations(currentSongId, excludeIds.joinToString(","), limit)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun logPlayEvent(playEvent: com.premium.spotifyclone.data.network.PlayEvent) {
        withContext(Dispatchers.IO) {
            try {
                apiService.logPlay(playEvent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getTracksByTag(tag: String, limit: Int = 20): List<Track> {
        return withContext(Dispatchers.IO) {
            try {
                // Use the new unified search for tags
                apiService.searchMusic(tag)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun searchTracks(query: String, limit: Int = 20): List<Track> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.searchMusic(query)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun searchAll(query: String): com.premium.spotifyclone.data.network.SearchAllResponse? {
        return withContext(Dispatchers.IO) {
            try {
                apiService.searchAll(query)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getArtistTracks(name: String): List<Track> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getArtistTracks(name)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getAlbumTracks(id: String): List<Track> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getAlbumTracks(id)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getRecommendedSongs(id: String): List<Track> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getRecommendedSongs(id)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getPlaylistTracks(id: String): List<Track> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getPlaylistTracks(id)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getStreamUrl(searchKey: String): StreamResponse? {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getStreamUrl(searchKey)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
