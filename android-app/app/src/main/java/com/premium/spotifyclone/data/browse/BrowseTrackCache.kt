package com.premium.spotifyclone.data.browse

import com.premium.spotifyclone.data.models.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds tracks last loaded on Home so Search can query them without an extra network round-trip.
 * Updated when [com.premium.spotifyclone.ui.viewmodel.HomeScreenViewModel] successfully fetches feeds.
 */
object BrowseTrackCache {
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    fun replaceFromHome(recent: List<Track>, forYou: List<Track>) {
        val merged = LinkedHashMap<String, Track>()
        recent.forEach { merged[it.id] = it }
        forYou.forEach { merged.putIfAbsent(it.id, it) }
        _tracks.value = merged.values.toList()
    }
}
