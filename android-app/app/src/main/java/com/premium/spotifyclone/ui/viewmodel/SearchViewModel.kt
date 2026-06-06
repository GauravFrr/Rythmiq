package com.premium.spotifyclone.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.premium.spotifyclone.data.browse.BrowseTrackCache
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.data.repository.LikedTracksRepository
import com.premium.spotifyclone.data.repository.SearchHistoryRepository
import com.premium.spotifyclone.data.network.SearchEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import com.premium.spotifyclone.data.repository.MusicRepository
import com.premium.spotifyclone.data.network.RetrofitInstance

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val likedRepository: LikedTracksRepository,
    private val historyRepository: SearchHistoryRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _query.asStateFlow()

    fun setQuery(value: String) {
        _query.value = value
    }

    private val debouncedQuery = _query
        .debounce(400L) // Wait a bit longer to avoid spamming the free Jamendo API
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val results: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _searchArtists = MutableStateFlow<List<SearchEntity>>(emptyList())
    val searchArtists: StateFlow<List<SearchEntity>> = _searchArtists.asStateFlow()

    private val _searchAlbums = MutableStateFlow<List<SearchEntity>>(emptyList())
    val searchAlbums: StateFlow<List<SearchEntity>> = _searchAlbums.asStateFlow()

    private val _searchPlaylists = MutableStateFlow<List<SearchEntity>>(emptyList())
    val searchPlaylists: StateFlow<List<SearchEntity>> = _searchPlaylists.asStateFlow()

    init {
        viewModelScope.launch {
            debouncedQuery.collect { q ->
                if (q.isBlank()) {
                    _searchResults.value = emptyList()
                    _searchArtists.value = emptyList()
                    _searchAlbums.value = emptyList()
                    _searchPlaylists.value = emptyList()
                } else {
                    val res = musicRepository.searchAll(q)
                    if (res != null) {
                        _searchResults.value = res.songs
                        _searchArtists.value = res.artists
                        _searchAlbums.value = res.albums
                        _searchPlaylists.value = res.playlists
                    } else {
                        _searchResults.value = emptyList()
                        _searchArtists.value = emptyList()
                        _searchAlbums.value = emptyList()
                        _searchPlaylists.value = emptyList()
                    }
                }
            }
        }
    }

    suspend fun fetchArtistTracks(name: String): List<Track> {
        return musicRepository.getArtistTracks(name)
    }

    suspend fun fetchAlbumTracks(id: String): List<Track> {
        return musicRepository.getAlbumTracks(id)
    }

    suspend fun fetchPlaylistTracks(id: String): List<Track> {
        return musicRepository.getPlaylistTracks(id)
    }

    val searchHistory: StateFlow<List<Track>> = historyRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addToHistory(track: Track) {
        viewModelScope.launch {
            historyRepository.addToHistory(track)
        }
    }

    fun removeFromHistory(trackId: String) {
        viewModelScope.launch {
            historyRepository.removeFromHistory(trackId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    val categories = listOf(
        SearchCategory("1", "Music", Color(0xFFE91E63)),
        SearchCategory("2", "Podcasts", Color(0xFF009688)),
        SearchCategory("3", "Live Events", Color(0xFF673AB7)),
        SearchCategory("4", "Home of I-Pop", Color(0xFF4CAF50)),
        SearchCategory("5", "Made For You", Color(0xFF2196F3)),
        SearchCategory("6", "New Releases", Color(0xFFFF5722)),
        SearchCategory("7", "Hindi", Color(0xFFFFC107)),
        SearchCategory("8", "Punjabi", Color(0xFF795548)),
    )

    companion object {
        fun factory(
            likedRepository: LikedTracksRepository,
            historyRepository: SearchHistoryRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(SearchViewModel::class.java))
                    val musicRepo = MusicRepository(RetrofitInstance.api)
                    return SearchViewModel(likedRepository, historyRepository, musicRepo) as T
                }
            }
    }
}

data class SearchCategory(
    val id: String,
    val title: String,
    val color: Color
)
