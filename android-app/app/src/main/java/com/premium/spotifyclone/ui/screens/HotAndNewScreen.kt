package com.premium.spotifyclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
class HotAndNewViewModel : ViewModel() {
    private val repo = MusicRepository(RetrofitInstance.api)

    var trendingIndia by mutableStateOf<List<Track>>(emptyList())
        private set
    var globalTop by mutableStateOf<List<Track>>(emptyList())
        private set
    var viralDrops by mutableStateOf<List<Track>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set

    init { fetch() }

    fun fetch() {
        viewModelScope.launch {
            isLoading = true
            try {
                trendingIndia = repo.getTracksByTag("trending regional hits 2026", 15)
                globalTop     = repo.getTracksByTag("trending global hits 2026", 15)
                viralDrops    = repo.getTracksByTag("viral hits 2026", 15)
            } catch (e: Exception) { e.printStackTrace() }
            isLoading = false
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(c: Class<T>) = HotAndNewViewModel() as T
        }
    }
}

// ── Screen ─────────────────────────────────────────────────────────────────
@Composable
fun HotAndNewScreen(
    modifier: Modifier = Modifier,
    onPlayTracks: (List<Track>) -> Unit = {}
) {
    val vm: HotAndNewViewModel = viewModel(factory = HotAndNewViewModel.Factory)

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

        // ── Trending India ──────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HotSectionHeader("Trending Regional", Icons.Default.TrendingUp)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(vm.trendingIndia, key = { "india-${it.id}" }) { track ->
            SongListRow(
                track = track,
                onClick = { onPlayTracks(playWindowFromList(vm.trendingIndia, track)) },
                onMoreClick = {}
            )
        }

        // ── Global Top Hits ─────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(24.dp))
            HotSectionHeader("Trending Global", Icons.Default.Public)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(vm.globalTop, key = { "global-${it.id}" }) { track ->
            SongListRow(
                track = track,
                onClick = { onPlayTracks(playWindowFromList(vm.globalTop, track)) },
                onMoreClick = {}
            )
        }

        // ── Viral Drops ─────────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(24.dp))
            HotSectionHeader("Viral Hits", Icons.Default.Whatshot)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(vm.viralDrops, key = { "viral-${it.id}" }) { track ->
            SongListRow(
                track = track,
                onClick = { onPlayTracks(playWindowFromList(vm.viralDrops, track)) },
                onMoreClick = {}
            )
        }
    }
}

@Composable
private fun HotSectionHeader(title: String, icon: ImageVector) {
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
