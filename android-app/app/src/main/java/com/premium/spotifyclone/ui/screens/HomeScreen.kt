package com.premium.spotifyclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.premium.spotifyclone.data.local.AppDatabase
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.data.repository.RecentlyPlayedRepository
import com.premium.spotifyclone.ui.components.SongListRow
import com.premium.spotifyclone.ui.theme.*
import com.premium.spotifyclone.ui.utils.playWindowFromList
import com.premium.spotifyclone.ui.viewmodel.HomeScreenViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onPlayTracks: (List<Track>) -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val recentRepo = remember {
        RecentlyPlayedRepository(AppDatabase.getInstance(context).recentlyPlayedDao())
    }
    val viewModel: HomeScreenViewModel = viewModel(
        factory = HomeScreenViewModel.factory(recentRepo)
    )

    // Pass Firebase UID to ViewModel so backend can personalize the feed
    LaunchedEffect(Unit) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        viewModel.setUserId(uid)
        viewModel.fetchRecommendations()
    }

    val recentTracks by viewModel.recentlyPlayed.collectAsState()
    val recommended by viewModel.recommended
    val isLoading by viewModel.isLoadingRecs

    val playAndTrack: (Track, List<Track>) -> Unit = { track, list ->
        viewModel.trackPlayed(track, source = "home")
        onPlayTracks(playWindowFromList(list, track))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppBlack),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ── Recently Played Quick Grid ──────────────────────────────────────
        if (recentTracks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "Jump Back In")
                Spacer(modifier = Modifier.height(12.dp))
                RecentGrid(
                    tracks = recentTracks.take(6),
                    onClick = { t -> playAndTrack(t, recentTracks) }
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // ── Recommended Songs Header ────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (com.premium.spotifyclone.data.api.BackendApiClient.authToken != null) "Made For You" else "Recommended Songs",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Infinite Badge
                Box(
                    modifier = Modifier
                        .background(AccentRed.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "∞ Infinite",
                        color = AccentRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Refresh button
                IconButton(
                    onClick = { viewModel.fetchRecommendations() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
            // Personalized label if logged in
            if (com.premium.spotifyclone.data.api.BackendApiClient.authToken != null) {
                Text(
                    text = "Based on your listening history",
                    color = AccentRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Recommended Song List Rows ──────────────────────────────────────
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentRed, modifier = Modifier.size(32.dp))
                }
            }
        } else if (recommended.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Listen to some songs to get personalized recommendations!",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(recommended, key = { it.id }) { track ->
                SongListRow(
                    track = track,
                    onClick = { playAndTrack(track, recommended) },
                    onLikeClick = null,
                    onMoreClick = { /* TODO */ }
                )
            }
        }
    }
}

// ── Section header ──────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = TextPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 16.dp)
    )
}

// ── Recent Tracks 2-col compact grid ───────────────────────────────────────
@Composable
private fun RecentGrid(tracks: List<Track>, onClick: (Track) -> Unit) {
    val rows = tracks.chunked(2)
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { track ->
                    RecentCard(
                        track = track,
                        onClick = { onClick(track) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RecentCard(track: Track, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppElevated)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = track.title,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .weight(1f)
        )
    }
}
