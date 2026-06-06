package com.premium.spotifyclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.premium.spotifyclone.SpotifyCloneApplication
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.data.repository.LikedTracksRepository
import com.premium.spotifyclone.data.repository.PlaylistRepository
import com.premium.spotifyclone.ui.components.PlaylistCoverCollage
import com.premium.spotifyclone.ui.components.PlaylistStyleActionRow
import com.premium.spotifyclone.ui.viewmodel.PlaylistDetailViewModel
import com.premium.spotifyclone.ui.utils.playWindowFromList

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    onPlayTracks: (List<Track>) -> Unit,
    modifier: Modifier = Modifier,
    shuffleEnabled: Boolean = false,
    onToggleShuffle: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpotifyCloneApplication
    val likedRepo = remember(context) { LikedTracksRepository(app.database) }
    val playlistRepo = remember(context) { PlaylistRepository(app.database) }
    val viewModel: PlaylistDetailViewModel = viewModel(
        key = playlistId,
        factory = PlaylistDetailViewModel.factory(playlistId, playlistRepo, likedRepo)
    )
    val title by viewModel.title.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val likedTracks by viewModel.likedTracks.collectAsStateWithLifecycle()
    val browseTracks by viewModel.browseTracks.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var renameField by remember { mutableStateOf("") }

    val spotifyGreen = Color(0xFF1DB954)

    val addCandidates = remember(likedTracks, browseTracks, tracks) {
        val inP = tracks.map { it.id }.toSet()
        (likedTracks + browseTracks)
            .asSequence()
            .filter { it.audioUrl != null }
            .distinctBy { it.id }
            .filter { it.id !in inP }
            .sortedBy { it.title.lowercase() }
            .toList()
    }

    val playableTracks = remember(tracks) { tracks.filter { it.audioUrl != null } }
    val thumbCovers = remember(tracks) {
        tracks.take(4).map { t -> t.coverUrl.takeIf { it.isNotBlank() } }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                PlaylistHeroHeader(
                    title = title,
                    trackCount = tracks.size,
                    tracks = tracks,
                    onBack = onBack,
                    onRename = {
                        renameField = title
                        showRenameDialog = true
                    },
                    onAdd = { showAddDialog = true },
                    onDeleteRequest = { showDeleteConfirm = true },
                )
            }

            item {
                PlaylistStyleActionRow(
                    thumbCoverUrls = thumbCovers,
                    playableTracks = playableTracks,
                    shuffleEnabled = shuffleEnabled,
                    onPlayPlaylist = { onPlayTracks(playableTracks) },
                    onToggleShuffle = onToggleShuffle,
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { showAddDialog = true },
                        label = { Text("Add", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF2A2A2A),
                            labelColor = Color.White,
                        ),
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            renameField = title
                            showRenameDialog = true
                        },
                        label = { Text("Edit", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF2A2A2A),
                            labelColor = Color.White,
                        ),
                    )
                    FilterChip(
                        selected = false,
                        onClick = { /* reserved */ },
                        label = { Text("Sort", fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF2A2A2A),
                            labelColor = Color(0xFF888888),
                        ),
                    )
                }
            }

            item {
                HorizontalDivider(color = Color(0xFF2A2A2A))
            }

            if (tracks.isEmpty()) {
                item {
                    Text(
                        text = "Tap Add to add songs from your likes.",
                        color = Color(0xFF888888),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            } else {
                items(tracks, key = { it.id }) { track ->
                    PlaylistDetailTrackRow(
                        track = track,
                        onPlay = { onPlayTracks(playWindowFromList(tracks, track)) },
                        onRemove = { viewModel.removeTrack(track.id) },
                    )
                    HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 1.dp)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete playlist?", color = Color.White) },
            text = {
                Text(
                    "\"$title\" will be removed. This cannot be undone.",
                    color = Color(0xFFB3B3B3),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deletePlaylist(onBack)
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color(0xFFB3B3B3))
                }
            },
            containerColor = Color(0xFF2A2A2A),
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename playlist", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renameField,
                    onValueChange = { renameField = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = spotifyGreen,
                        unfocusedBorderColor = Color(0xFF555555),
                        cursorColor = spotifyGreen,
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renamePlaylist(renameField)
                        showRenameDialog = false
                    }
                ) {
                    Text("Save", color = spotifyGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color(0xFFB3B3B3))
                }
            },
            containerColor = Color(0xFF2A2A2A)
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add songs", color = Color.White) },
            text = {
                if (addCandidates.isEmpty()) {
                    Text(
                        "Nothing to add from likes or Home. Like some songs or open Home so feeds load, and add tracks that are not already in this playlist.",
                        color = Color(0xFFB3B3B3)
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(320.dp)) {
                        items(addCandidates, key = { it.id }) { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addTrack(track)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = track.coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF333333)),
                                    contentScale = ContentScale.Crop
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 10.dp)
                                ) {
                                    Text(track.title, color = Color.White, fontSize = 15.sp, maxLines = 1)
                                    Text(track.artist, color = Color(0xFFB3B3B3), fontSize = 13.sp, maxLines = 1)
                                }
                            }
                            HorizontalDivider(color = Color(0xFF2A2A2A))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Done", color = Color(0xFF1DB954))
                }
            },
            containerColor = Color(0xFF2A2A2A)
        )
    }
}

@Composable
private fun PlaylistHeroHeader(
    title: String,
    trackCount: Int,
    tracks: List<Track>,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onAdd: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val gradientColors = listOf(
        Color(0xFF4A1F2E),
        Color(0xFF1A1216),
        Color(0xFF121212),
    )
    val coverUrls = remember(tracks) {
        tracks.take(4).map { t -> t.coverUrl.takeIf { it.isNotBlank() } }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(gradientColors)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color.White)
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add songs", tint = Color.White)
            }
            IconButton(onClick = onDeleteRequest) {
                Icon(Icons.Default.Delete, contentDescription = "Delete playlist", tint = Color(0xFFE53935))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            PlaylistCoverCollage(
                coverUrls = coverUrls,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$trackCount songs",
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PlaylistDetailTrackRow(
    track: Track,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember(track.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = track.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF333333)),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                color = Color(0xFFB3B3B3),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color(0xFFB3B3B3),
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = Color(0xFF2A2A2A),
            ) {
                DropdownMenuItem(
                    text = { Text("Remove from playlist", color = Color(0xFFE57373)) },
                    onClick = {
                        menuOpen = false
                        onRemove()
                    },
                )
            }
        }
    }
}
