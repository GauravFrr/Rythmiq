package com.premium.spotifyclone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.premium.spotifyclone.data.local.PlaylistEntity
import com.premium.spotifyclone.data.local.PlaylistWithCovers
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.data.repository.LikedTracksRepository
import com.premium.spotifyclone.data.repository.PlaylistRepository
import com.premium.spotifyclone.ui.models.PlaylistUiModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val likedRepository: LikedTracksRepository,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    val likedTracks: StateFlow<List<Track>> = likedRepository.observeLikedTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<PlaylistUiModel>> = playlistRepository.observePlaylistsWithCovers()
        .map { items ->
            items.map { item ->
                PlaylistUiModel(
                    id = item.playlist.id,
                    name = item.playlist.name,
                    coverUrls = item.tracks.take(4).map { it.coverUrl }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createPlaylist(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = playlistRepository.createPlaylist(name)
            onCreated(id)
        }
    }

    companion object {
        fun factory(
            likedRepository: LikedTracksRepository,
            playlistRepository: PlaylistRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(LibraryViewModel::class.java))
                    return LibraryViewModel(likedRepository, playlistRepository) as T
                }
            }
    }
}
