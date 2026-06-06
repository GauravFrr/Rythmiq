package com.premium.spotifyclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.data.network.RetrofitInstance
import com.premium.spotifyclone.data.repository.MusicRepository
import com.premium.spotifyclone.ui.components.SongListRow
import com.premium.spotifyclone.ui.theme.*
import com.premium.spotifyclone.ui.utils.playWindowFromList
import kotlinx.coroutines.launch

// ── ViewModel ──────────────────────────────────────────────────────────────
class ChartsViewModel : ViewModel() {
    private val repo = MusicRepository(RetrofitInstance.api)

    var indiaTop50    by mutableStateOf<List<Track>>(emptyList()); private set
    var globalTop50   by mutableStateOf<List<Track>>(emptyList()); private set
    var aoty          by mutableStateOf<List<Track>>(emptyList()); private set
    var popularArtists by mutableStateOf<List<Track>>(emptyList()); private set
    var popularSongs  by mutableStateOf<List<Track>>(emptyList()); private set
    var isLoading     by mutableStateOf(true);                      private set

    init { fetch() }

    fun fetch() {
        viewModelScope.launch {
            isLoading = true
            try {
                indiaTop50  = repo.getTracksByTag("new songs 2026", 15)
                globalTop50 = repo.getTracksByTag("global hits 2026", 15)
                aoty        = repo.getTracksByTag("new trending albums 2026", 15)
                popularArtists = repo.getTracksByTag("popular artists 2026", 15)
                popularSongs = repo.getTracksByTag("popular songs 2026", 15)
            } catch (e: Exception) { e.printStackTrace() }
            isLoading = false
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(c: Class<T>) = ChartsViewModel() as T
        }
    }
}

// ── Screen ─────────────────────────────────────────────────────────────────
@Composable
fun ChartsScreen(
    modifier: Modifier = Modifier,
    onPlayTracks: (List<Track>) -> Unit = {}
) {
    val vm: ChartsViewModel = viewModel(factory = ChartsViewModel.Factory)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppBlack),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        if (vm.isLoading) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentRed)
                }
            }
            return@LazyColumn
        }

        // ── India Top 50 ────────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(16.dp))
            ChartsSectionHeader(icon = Icons.Default.LocalFireDepartment, title = "New Releases 2026")
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(vm.indiaTop50.take(15), key = { "in-${it.id}" }) { track ->
            val idx = vm.indiaTop50.indexOf(track) + 1
            SongListRow(
                track = track,
                numberPrefix = idx,
                onClick = { onPlayTracks(playWindowFromList(vm.indiaTop50, track)) },
                onMoreClick = {}
            )
        }

        // ── Global Top 50 ───────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(28.dp))
            ChartsSectionHeader(icon = Icons.Default.Public, title = "Global Hits 2026")
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(vm.globalTop50.take(15), key = { "gl-${it.id}" }) { track ->
            val idx = vm.globalTop50.indexOf(track) + 1
            SongListRow(
                track = track,
                numberPrefix = idx,
                onClick = { onPlayTracks(playWindowFromList(vm.globalTop50, track)) },
                onMoreClick = {}
            )
        }

        // ── AOTY — Album of the Year ─────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(28.dp))
            ChartsSectionHeader(icon = Icons.Default.Album, title = "New Trending Albums 2026")
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(vm.aoty.take(15), key = { "aoty-${it.id}" }) { track ->
            val idx = vm.aoty.indexOf(track) + 1
            SongListRow(
                track = track,
                numberPrefix = idx,
                onClick = { onPlayTracks(playWindowFromList(vm.aoty, track)) },
                onMoreClick = {}
            )
        }

        // ── Popular Artists ──────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(28.dp))
            ChartsSectionHeader(icon = Icons.Default.Mic, title = "Popular Artists")
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(vm.popularArtists.take(15), key = { "pa-${it.id}" }) { track ->
            val idx = vm.popularArtists.indexOf(track) + 1
            SongListRow(
                track = track,
                numberPrefix = idx,
                onClick = { onPlayTracks(playWindowFromList(vm.popularArtists, track)) },
                onMoreClick = {}
            )
        }

        // ── Popular Songs ────────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(28.dp))
            ChartsSectionHeader(icon = Icons.Default.Star, title = "Popular Songs")
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(vm.popularSongs.take(15), key = { "ps-${it.id}" }) { track ->
            val idx = vm.popularSongs.indexOf(track) + 1
            SongListRow(
                track = track,
                numberPrefix = idx,
                onClick = { onPlayTracks(playWindowFromList(vm.popularSongs, track)) },
                onMoreClick = {}
            )
        }
    }
}

@Composable
private fun ChartsSectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AccentRed, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
