package com.premium.spotifyclone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.premium.spotifyclone.data.browse.BrowseTrackCache
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.data.repository.LikedTracksRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LikedSongsDetailViewModel(
    private val likedRepository: LikedTracksRepository,
) : ViewModel() {

    val tracks: StateFlow<List<Track>> = likedRepository.observeLikedTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val browseTracks: StateFlow<List<Track>> = BrowseTrackCache.tracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val addCandidates: StateFlow<List<Track>> = combine(tracks, browseTracks) { liked, browse ->
        val likedIds = liked.map { it.id }.toSet()
        browse
            .asSequence()
            .filter { it.audioUrl != null && it.id !in likedIds }
            .distinctBy { it.id }
            .sortedBy { it.title.lowercase() }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addToLikes(track: Track) {
        viewModelScope.launch {
            if (track.audioUrl != null) {
                likedRepository.setLiked(track, true)
            }
        }
    }

    fun removeFromLikes(track: Track) {
        viewModelScope.launch {
            likedRepository.setLiked(track, false)
        }
    }

    companion object {
        fun factory(likedRepository: LikedTracksRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(LikedSongsDetailViewModel::class.java))
                    return LikedSongsDetailViewModel(likedRepository) as T
                }
            }
    }
}
