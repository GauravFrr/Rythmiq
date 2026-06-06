package com.premium.spotifyclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.premium.spotifyclone.data.api.JamendoApiService
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.ui.components.SongListRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyPlayedScreen(
    onNavigateBack: () -> Unit,
    onPlayTracks: (List<Track>) -> Unit
) {
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = JamendoApiService.create().getTracks(
                    clientId = "e749a2ee",
                    limit = 15,
                    order = "popularity_week"
                )
                tracks = response.results.map { jt ->
                    Track(
                        id = jt.id,
                        title = jt.name,
                        artist = jt.artistName,
                        durationMs = jt.duration * 1000L,
                        audioUrl = jt.audio,
                        coverUrl = jt.image
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recently Played", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1DB954))
            }
        } else if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No recently played tracks found.", color = Color(0xFFB3B3B3), fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(tracks) { track ->
                    SongListRow(
                        track = track,
                        onClick = { onPlayTracks(listOf(track)) },
                        onMoreClick = { /* No-op for now */ }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
