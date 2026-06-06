package com.premium.spotifyclone.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.data.network.RetrofitInstance
import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModelProvider
import com.premium.spotifyclone.data.repository.MusicRepository
import com.premium.spotifyclone.data.repository.RecentlyPlayedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest

class HomeScreenViewModel(
    private val repository: MusicRepository,
    private val recentRepository: RecentlyPlayedRepository
) : ViewModel() {

    private val _recentlyPlayed = MutableStateFlow<List<Track>>(emptyList())
    val recentlyPlayed: StateFlow<List<Track>> = _recentlyPlayed.asStateFlow()

    private val _recommended = mutableStateOf<List<Track>>(emptyList())
    val recommended: State<List<Track>> = _recommended

    private val _isLoadingRecs = mutableStateOf(true)
    val isLoadingRecs: State<Boolean> = _isLoadingRecs

    // Current auth token — set from outside (MainActivity or NavGraph)
    private var authToken: String? = null
    private var userId: String? = null

    init {
        observeLocalHistory()
    }

    /** Called by the UI layer after the user logs in to enable personalized recs */
    fun setAuthToken(token: String?) {
        authToken = token
        fetchRecommendations()
    }

    /** Set the Firebase UID for personalized recommendations */
    fun setUserId(uid: String?) {
        userId = uid
    }

    private fun observeLocalHistory() {
        viewModelScope.launch {
            recentRepository.observeRecentlyPlayed().collectLatest { history ->
                _recentlyPlayed.value = history
            }
        }
    }

    fun fetchRecommendations() {
        viewModelScope.launch {
            _isLoadingRecs.value = true
            try {
                val tracks = repository.getPersonalizedFeed(userId = userId)
                    .distinctBy { it.id }
                    .distinctBy { "${it.title.lowercase()}-${it.artist.lowercase()}" }
                _recommended.value = tracks
            } catch (e: Exception) {
                e.printStackTrace()
                _recommended.value = emptyList()
            } finally {
                _isLoadingRecs.value = false
            }
        }
    }

    /** Called when a user plays a track — logs to local DB and to backend (if logged in) */
    fun trackPlayed(track: Track, source: String = "home") {
        viewModelScope.launch {
            // Always save to local recently-played
            recentRepository.addTrack(track)

            // If logged in, also log to backend for personalized recommendations
            val token = authToken
            if (token != null) {
                try {
                    repository.logPlayEvent(
                        com.premium.spotifyclone.data.network.PlayEvent(
                            songId = track.id,
                            songName = track.title,
                            artistId = null,
                            artistName = track.artist,
                            albumId = null,
                            genre = null,
                            language = null,
                            duration = track.durationMs.toInt(),
                            listenedDuration = 0, // This is just a click event from home, proper duration is logged in PlayerService
                            skipped = false,
                            liked = false,
                            addedToPlaylist = false,
                            hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
                            dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK),
                            source = source
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace() // Don't crash — logging is non-critical
                }
            }
        }
    }

    companion object {
        fun factory(recentRepo: RecentlyPlayedRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val apiService = RetrofitInstance.api
                    val repository = MusicRepository(apiService)
                    return HomeScreenViewModel(repository, recentRepo) as T
                }
            }
    }
}
