package com.premium.spotifyclone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.premium.spotifyclone.data.browse.BrowseTrackCache
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.data.repository.LikedTracksRepository
import com.premium.spotifyclone.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(
    private val playlistId: String,
    private val playlistRepository: PlaylistRepository,
    likedRepository: LikedTracksRepository,
) : ViewModel() {

    private val _title = MutableStateFlow("Playlist")
    val title: StateFlow<String> = _title.asStateFlow()

    val tracks: StateFlow<List<Track>> = playlistRepository.observePlaylistTracks(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val likedTracks: StateFlow<List<Track>> = likedRepository.observeLikedTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val browseTracks: StateFlow<List<Track>> = BrowseTrackCache.tracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _title.value = playlistRepository.getPlaylistName(playlistId) ?: "Playlist"
        }
    }

    fun addTrack(track: Track) {
        viewModelScope.launch { playlistRepository.addTrack(playlistId, track) }
    }

    fun removeTrack(trackId: String) {
        viewModelScope.launch { playlistRepository.removeTrack(playlistId, trackId) }
    }

    fun renamePlaylist(newName: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(playlistId, newName)
            _title.value = playlistRepository.getPlaylistName(playlistId) ?: newName.trim()
        }
    }

    fun deletePlaylist(onDeleted: () -> Unit) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            onDeleted()
        }
    }

    companion object {
        fun factory(
            playlistId: String,
            playlistRepository: PlaylistRepository,
            likedRepository: LikedTracksRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(PlaylistDetailViewModel::class.java))
                    return PlaylistDetailViewModel(playlistId, playlistRepository, likedRepository) as T
                }
            }
    }
}
