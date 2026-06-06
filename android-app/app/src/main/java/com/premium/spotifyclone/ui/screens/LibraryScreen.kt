package com.premium.spotifyclone.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.premium.spotifyclone.SpotifyCloneApplication
import com.premium.spotifyclone.data.repository.LikedTracksRepository
import com.premium.spotifyclone.data.repository.PlaylistRepository
import com.premium.spotifyclone.ui.components.PlaylistCoverCollage
import com.premium.spotifyclone.ui.navigation.LikedSongsDetailRoute
import com.premium.spotifyclone.ui.navigation.PlaylistDetailRoute
import com.premium.spotifyclone.ui.navigation.Screen
import com.premium.spotifyclone.ui.viewmodel.LibraryViewModel
import com.premium.spotifyclone.ui.models.PlaylistUiModel
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.ui.theme.*

private enum class LibraryFilter { Playlists, Albums, Artists }
private enum class LibrarySort { Recent, Alphabetical }

@Composable
fun LibraryScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    onPlayTracks: (List<Track>) -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpotifyCloneApplication
    val likedRepo = remember(context) { LikedTracksRepository(app.database) }
    val playlistRepo = remember(context) { PlaylistRepository(app.database) }
    val viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(likedRepo, playlistRepo))
    
    val likedTracks by viewModel.likedTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LibraryFilter.Playlists) }
    var sort by remember { mutableStateOf(LibrarySort.Recent) }
    var gridView by remember { mutableStateOf(true) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val displayPlaylists = remember(playlists, sort) {
        when (sort) {
            LibrarySort.Recent -> playlists
            LibrarySort.Alphabetical -> playlists.sortedBy { it.name.lowercase() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBlack)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Your Library",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Row {
                IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search", tint = TextPrimary)
                }
                IconButton(onClick = {
                    newPlaylistName = ""
                    showCreatePlaylist = true
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Create playlist", tint = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Filters ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val chips = listOf(
                LibraryFilter.Playlists to "Playlists",
                LibraryFilter.Albums to "Albums",
                LibraryFilter.Artists to "Artists"
            )
            chips.forEach { (f, label) ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(label, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentRed.copy(alpha = 0.2f),
                        selectedLabelColor = AccentRed,
                        containerColor = AppElevated,
                        labelColor = TextSecondary,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = filter == f,
                        borderColor = Color.Transparent,
                        selectedBorderColor = AccentRed
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Sort & View Toggle ──────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(Icons.Rounded.Sort, contentDescription = "Sort", tint = TextPrimary)
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    containerColor = AppElevated,
                ) {
                    DropdownMenuItem(
                        text = { Text("Recently added", color = TextPrimary) },
                        onClick = { sort = LibrarySort.Recent; sortMenuExpanded = false },
                    )
                    DropdownMenuItem(
                        text = { Text("Alphabetical", color = TextPrimary) },
                        onClick = { sort = LibrarySort.Alphabetical; sortMenuExpanded = false },
                    )
                }
            }
            Text(
                text = when (sort) {
                    LibrarySort.Recent -> "Recents"
                    LibrarySort.Alphabetical -> "Alphabetical"
                },
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clickable { sortMenuExpanded = true },
            )
            Spacer(modifier = Modifier.weight(1f))
            if (filter == LibraryFilter.Playlists) {
                IconButton(onClick = { gridView = !gridView }) {
                    Icon(
                        imageVector = if (gridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                        contentDescription = if (gridView) "List view" else "Grid view",
                        tint = TextPrimary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Content Area with Animations ────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .animateContentSize(animationSpec = tween(400))
        ) {
            when (filter) {
                LibraryFilter.Playlists -> {
                    if (gridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            item(span = { GridItemSpan(2) }) {
                                LikedSongsGridCard(
                                    songCount = likedTracks.size,
                                    onClick = { navController.navigate(LikedSongsDetailRoute.route) },
                                )
                            }
                            lazyGridItems(displayPlaylists, key = { it.id }) { pl ->
                                PlaylistGridTile(
                                    playlist = pl,
                                    onClick = { navController.navigate(PlaylistDetailRoute.route(pl.id)) },
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                LikedSongsListRow(
                                    songCount = likedTracks.size,
                                    onClick = { navController.navigate(LikedSongsDetailRoute.route) },
                                )
                                Divider(color = AppElevated)
                            }
                            items(displayPlaylists, key = { it.id }) { pl ->
                                PlaylistListRow(
                                    playlist = pl,
                                    onClick = { navController.navigate(PlaylistDetailRoute.route(pl.id)) },
                                )
                                Divider(color = AppElevated)
                            }
                        }
                    }
                }
                LibraryFilter.Albums -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Albums will show here in a future update.", color = TextSecondary, fontSize = 15.sp)
                    }
                }
                LibraryFilter.Artists -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Artists will show here in a future update.", color = TextSecondary, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    // ── Create Playlist Dialog ──────────────────────────────────
    if (showCreatePlaylist) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text("New playlist", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    singleLine = true,
                    placeholder = { Text("Playlist name", color = TextSecondary) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = AccentRed,
                        unfocusedIndicatorColor = TextSecondary,
                        cursorColor = AccentRed,
                        focusedContainerColor = AppElevated,
                        unfocusedContainerColor = AppElevated
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createPlaylist(newPlaylistName) { id ->
                            showCreatePlaylist = false
                            navController.navigate(PlaylistDetailRoute.route(id))
                        }
                    }
                ) {
                    Text("Create", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylist = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = AppElevated
        )
    }
}

@Composable
private fun LikedSongsGridCard(
    songCount: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.2f)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF450af5), Color(0xFFc4efd9))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Liked Songs",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Rounded.PushPin,
                contentDescription = null,
                tint = AccentRed,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "Playlist · $songCount songs",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PlaylistGridTile(
    playlist: PlaylistUiModel,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(AppElevated),
            contentAlignment = Alignment.Center,
        ) {
            PlaylistCoverCollage(
                coverUrls = playlist.coverUrls,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
        )
        Text(
            text = "Playlist",
            color = TextSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun LikedSongsListRow(
    songCount: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF450af5), Color(0xFFc4efd9))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = "Liked Songs",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Rounded.PushPin,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Playlist · $songCount songs",
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun PlaylistListRow(
    playlist: PlaylistUiModel,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppElevated),
            contentAlignment = Alignment.Center,
        ) {
            PlaylistCoverCollage(
                coverUrls = playlist.coverUrls,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = playlist.name,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Playlist",
                color = TextSecondary,
                fontSize = 13.sp,
            )
        }
    }
}
