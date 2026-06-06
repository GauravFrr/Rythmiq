package com.premium.spotifyclone.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.premium.spotifyclone.SpotifyCloneApplication
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.data.network.FollowArtistRequest
import com.premium.spotifyclone.data.network.RetrofitInstance
import com.premium.spotifyclone.data.repository.LikedTracksRepository
import com.premium.spotifyclone.data.repository.SearchHistoryRepository
import com.premium.spotifyclone.ui.viewmodel.SearchCategory
import com.premium.spotifyclone.ui.viewmodel.SearchViewModel
import com.premium.spotifyclone.ui.utils.playWindowFromList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onPlayTracks: (List<Track>) -> Unit = {},
    onAddToQueue: (Track) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as? SpotifyCloneApplication ?: return
    val likedRepo = remember(context) { LikedTracksRepository(app.database) }
    val historyRepo = remember(context) { SearchHistoryRepository(app.database) }
    val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.factory(likedRepo, historyRepo))
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val searchArtists by viewModel.searchArtists.collectAsStateWithLifecycle()
    val searchAlbums by viewModel.searchAlbums.collectAsStateWithLifecycle()
    val searchPlaylists by viewModel.searchPlaylists.collectAsStateWithLifecycle()
    val history by viewModel.searchHistory.collectAsStateWithLifecycle()

    val spotifyGreen = Color(0xFF1DB954)
    var isFocused by remember { mutableStateOf(false) }
    var selectedChip by remember { mutableStateOf("Songs") }
    val categories = viewModel.categories
    var detailEntity by remember { mutableStateOf<DetailEntity?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }
    var showPlaylistSelection by remember { mutableStateOf(false) }
    var trackForPlaylistSelection by remember { mutableStateOf<Track?>(null) }

    BackHandler(enabled = detailEntity != null) {
        detailEntity = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        AnimatedVisibility(visible = detailEntity == null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = "Search",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Cursive
                        )
                    }
                    IconButton(onClick = { /* camera search not implemented */ }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera search", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    singleLine = true,
                    placeholder = {
                        Text(
                            "What do you want to listen to?",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Cursive
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = Color.White,
                        focusedContainerColor = Color(0xFF2A2A2A),
                        unfocusedContainerColor = Color(0xFF2A2A2A),
                        focusedPlaceholderColor = Color.White,
                        unfocusedPlaceholderColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Body with stable switching
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = when {
                    detailEntity != null -> SearchState.Detail
                    query.isNotBlank() -> SearchState.Results
                    isFocused -> SearchState.Recents
                    else -> SearchState.Browse
                },
                transitionSpec = {
                    fadeIn().togetherWith(fadeOut())
                },
                label = "SearchStateTransition"
            ) { state ->
                when (state) {
                    SearchState.Browse -> {
                        // Just an empty black background as requested
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                    }
                    SearchState.Detail -> {
                        detailEntity?.let { entity ->
                            EntityDetailScreen(
                                entity = entity,
                                onBack = { detailEntity = null },
                                onPlayAll = {
                                    // Add to history and play
                                    if (entity.tracks.isNotEmpty()) {
                                        viewModel.addToHistory(entity.tracks.first())
                                        onPlayTracks(entity.tracks)
                                    }
                                },
                                onPlayTrack = { track ->
                                    viewModel.addToHistory(track)
                                    onPlayTracks(playWindowFromList(entity.tracks, track))
                                }
                            )
                        }
                    }
                    SearchState.Recents -> {
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recents", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(
                                    "Clear all",
                                    color = Color(0xFFB3B3B3),
                                    fontSize = 14.sp,
                                    modifier = Modifier.clickable { viewModel.clearHistory() }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(history) { track ->
                                    RecentSearchRow(
                                        track = track,
                                        onPlay = { onPlayTracks(listOf(track)) },
                                        onDelete = { viewModel.removeFromHistory(track.id) },
                                        onOptionsClick = { selectedTrackForOptions = track }
                                    )
                                }
                            }
                        }
                    }
                    SearchState.Results -> {
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                            SearchFilterChips(
                                selectedChip = selectedChip,
                                onChipSelected = { selectedChip = it }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            when (selectedChip) {
                                "Songs" -> {
                                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                        items(results, key = { it.id }) { track ->
                                            SearchResultRow(
                                                track = track,
                                                onClick = {
                                                    viewModel.addToHistory(track)
                                                    onPlayTracks(playWindowFromList(results, track))
                                                },
                                                onOptionsClick = { selectedTrackForOptions = track }
                                            )
                                        }
                                    }
                                }
                                "Artists" -> {
                                    if (isLoadingDetails) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = spotifyGreen)
                                        }
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                            items(searchArtists) { artist ->
                                                ArtistRow(
                                                    name = artist.title,
                                                    imageUrl = artist.imageUrl,
                                                    onClick = {
                                                        isLoadingDetails = true
                                                        coroutineScope.launch {
                                                            val tracks = viewModel.fetchArtistTracks(artist.title)
                                                            isLoadingDetails = false
                                                            detailEntity = DetailEntity(
                                                                id = artist.id,
                                                                type = "artist",
                                                                title = artist.title,
                                                                subtitle = "Artist",
                                                                imageUrl = artist.imageUrl,
                                                                tracks = tracks,
                                                                isCircle = true
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                "Albums" -> {
                                    if (isLoadingDetails) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = spotifyGreen)
                                        }
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                            items(searchAlbums) { album ->
                                                AlbumRow(
                                                    title = album.title,
                                                    artist = album.subtitle,
                                                    imageUrl = album.imageUrl,
                                                    onClick = {
                                                        isLoadingDetails = true
                                                        coroutineScope.launch {
                                                            val tracks = viewModel.fetchAlbumTracks(album.id)
                                                            isLoadingDetails = false
                                                            detailEntity = DetailEntity(
                                                                id = album.id,
                                                                type = "album",
                                                                title = album.title,
                                                                subtitle = album.subtitle,
                                                                imageUrl = album.imageUrl,
                                                                tracks = tracks,
                                                                isCircle = false
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                "Playlists" -> {
                                    if (isLoadingDetails) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = spotifyGreen)
                                        }
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                            items(searchPlaylists) { playlist ->
                                                PlaylistRow(
                                                    title = playlist.title,
                                                    subtitle = playlist.subtitle,
                                                    imageUrl = playlist.imageUrl,
                                                    onClick = {
                                                        isLoadingDetails = true
                                                        coroutineScope.launch {
                                                            val tracks = viewModel.fetchPlaylistTracks(playlist.id)
                                                            isLoadingDetails = false
                                                            detailEntity = DetailEntity(
                                                                id = playlist.id,
                                                                type = "playlist",
                                                                title = playlist.title,
                                                                subtitle = playlist.subtitle,
                                                                imageUrl = playlist.imageUrl,
                                                                tracks = tracks,
                                                                isCircle = false
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No $selectedChip found.", color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Track Options
    if (selectedTrackForOptions != null) {
        val track = selectedTrackForOptions!!
        ModalBottomSheet(
            onDismissRequest = { selectedTrackForOptions = null },
            containerColor = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(track.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Cursive)
                        Text(track.artist, color = Color.Gray, fontSize = 14.sp, fontFamily = FontFamily.Cursive)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Options
                val optionModifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                
                Row(modifier = optionModifier.clickable { 
                    onAddToQueue(track)
                    selectedTrackForOptions = null
                }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Add to queue", color = Color.White, fontSize = 16.sp)
                }
                
                Row(modifier = optionModifier.clickable {
                    trackForPlaylistSelection = track
                    showPlaylistSelection = true
                    selectedTrackForOptions = null
                }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Add to playlist", color = Color.White, fontSize = 16.sp)
                }

                Row(modifier = optionModifier.clickable {
                    coroutineScope.launch {
                        likedRepo.setLiked(track, true)
                        selectedTrackForOptions = null
                    }
                }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Like this song", color = Color.White, fontSize = 16.sp)
                }

                Row(modifier = optionModifier.clickable {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "Listen to ${track.title} by ${track.artist} on Spotify Clone! ${track.audioUrl ?: ""}")
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Track"))
                    selectedTrackForOptions = null
                }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Share", color = Color.White, fontSize = 16.sp)
                }

                Row(modifier = optionModifier.clickable {
                    coroutineScope.launch {
                        try {
                            val authViewModel: com.premium.spotifyclone.viewmodel.AuthViewModel = androidx.lifecycle.ViewModelProvider(context as androidx.activity.ComponentActivity)[com.premium.spotifyclone.viewmodel.AuthViewModel::class.java]
                            if (authViewModel.isLoggedIn.value && track.artistId != null) {
                                com.premium.spotifyclone.data.network.RetrofitInstance.api.followArtist(
                                    com.premium.spotifyclone.data.network.FollowArtistRequest(
                                        artistId = track.artistId,
                                        artistName = track.artist,
                                        imageUrl = track.coverUrl
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        selectedTrackForOptions = null
                    }
                }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Follow ${track.artist}", color = Color.White, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showPlaylistSelection && trackForPlaylistSelection != null) {
        com.premium.spotifyclone.ui.components.PlaylistSelectionBottomSheet(
            track = trackForPlaylistSelection!!,
            onDismissRequest = { 
                showPlaylistSelection = false
                trackForPlaylistSelection = null 
            }
        )
    }
}

private enum class SearchState { Browse, Recents, Results, Detail }

data class DetailEntity(
    val id: String = "",
    val type: String = "artist", // "artist" | "album" | "playlist"
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val tracks: List<Track>,
    val isCircle: Boolean
)

@Composable
private fun RecentSearchRow(
    track: Track,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onOptionsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF282828)),
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium, 
                fontFamily = FontFamily.Cursive,
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist, 
                color = Color(0xFFB3B3B3), 
                fontSize = 14.sp, 
                fontFamily = FontFamily.Cursive,
                maxLines = 1
            )
        }
        val totalMs = track.durationMs
        val durationStr = "%d:%02d".format(totalMs / 60000, (totalMs % 60000) / 1000)
        Text(text = durationStr, color = Color(0xFFB3B3B3), fontSize = 14.sp, fontFamily = FontFamily.Cursive)
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onOptionsClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color(0xFFB3B3B3))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFB3B3B3), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SearchFilterChips(
    selectedChip: String,
    onChipSelected: (String) -> Unit
) {
    val chips = listOf("Songs", "Artists", "Albums", "Playlists")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(chips) { label ->
            FilterChip(
                selected = label == selectedChip,
                onClick = { onChipSelected(label) },
                label = { Text(label, fontSize = 13.sp, fontFamily = FontFamily.Cursive) },
                modifier = Modifier.height(32.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.White,
                    selectedLabelColor = Color.Black,
                    containerColor = Color(0xFF282828),
                    labelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color(0xFF404040),
                    selectedBorderColor = Color.Transparent,
                    enabled = true,
                    selected = label == selectedChip
                )
            )
        }
    }
}

@Composable
private fun SearchResultRow(
    track: Track,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF282828)),
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Cursive,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp,
                fontFamily = FontFamily.Cursive,
                maxLines = 1
            )
        }
        val totalMs = track.durationMs
        val durationStr = "%d:%02d".format(totalMs / 60000, (totalMs % 60000) / 1000)
        Text(text = durationStr, color = Color(0xFFB3B3B3), fontSize = 14.sp, fontFamily = FontFamily.Cursive)
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onOptionsClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color(0xFFB3B3B3))
        }
    }
}

@Composable
private fun ArtistRow(name: String, imageUrl: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0xFF282828)),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Cursive,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Artist",
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp,
                fontFamily = FontFamily.Cursive
            )
        }
    }
}

@Composable
private fun AlbumRow(title: String, artist: String, imageUrl: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF282828)),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Cursive,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Album • $artist",
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp,
                fontFamily = FontFamily.Cursive,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PlaylistRow(title: String, subtitle: String, imageUrl: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF282828)),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Cursive,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp,
                fontFamily = FontFamily.Cursive,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EntityDetailScreen(
    entity: DetailEntity,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayTrack: (Track) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Follow state
    var isFollowing by remember { mutableStateOf(false) }
    var followLoading by remember { mutableStateOf(false) }

    // Check follow status on load (only for artists)
    val isArtist = entity.type == "artist"
    LaunchedEffect(entity.id) {
        if (isArtist && com.premium.spotifyclone.data.api.BackendApiClient.authToken != null) {
            try {
                val response = RetrofitInstance.api.isFollowingArtist(
                    artistId = entity.id
                )
                isFollowing = response.following
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                // Blurred Background
                AsyncImage(
                    model = entity.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(40.dp),
                    contentScale = ContentScale.Crop
                )
                // Dark Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                )
                
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 12.dp, start = 8.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                
                // Central Content
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Circular Avatar
                    AsyncImage(
                        model = entity.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(160.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .border(4.dp, Color(0xFFE91E63), androidx.compose.foundation.shape.CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Title
                    Text(
                        text = entity.title,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Cursive,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Subtitle
                    Text(
                        text = "Premium selection • ${entity.tracks.size} tracks",
                        color = Color(0xFFB3B3B3),
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Cursive
                    )
                }
            }
        }

        item {
            Column {
                // Play All Action Button
                Button(
                    onClick = onPlayAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PLAY ALL", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // Follow / Unfollow button (only for artists when logged in)
                if (isArtist && com.premium.spotifyclone.data.api.BackendApiClient.authToken != null) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                followLoading = true
                                try {
                                    val response = if (isFollowing) {
                                        RetrofitInstance.api.unfollowArtist(
                                            body = FollowArtistRequest(entity.id, entity.title, entity.imageUrl)
                                        )
                                    } else {
                                        RetrofitInstance.api.followArtist(
                                            body = FollowArtistRequest(entity.id, entity.title, entity.imageUrl)
                                        )
                                    }
                                    isFollowing = response.following
                                } catch (e: Exception) { e.printStackTrace() }
                                finally { followLoading = false }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isFollowing) Color(0xFFE91E63) else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            color = if (isFollowing) Color(0xFFE91E63) else Color(0xFF888888)
                        ),
                        enabled = !followLoading
                    ) {
                        if (followLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFFE91E63), strokeWidth = 2.dp)
                        } else {
                            androidx.compose.animation.AnimatedContent(
                                targetState = isFollowing,
                                transitionSpec = {
                                    androidx.compose.animation.scaleIn(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.fadeIn() togetherWith
                                    androidx.compose.animation.scaleOut(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.fadeOut()
                                },
                                label = "follow_button_anim"
                            ) { following ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (following) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (following) "Following" else "Follow",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        itemsIndexed(entity.tracks, key = { _, track -> track.id }) { index, track ->
            DetailTrackRow(
                index = index + 1,
                track = track,
                onClick = { onPlayTrack(track) }
            )
        }
    }
}

@Composable
fun DetailTrackRow(index: Int, track: Track, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            color = Color(0xFFB3B3B3),
            fontSize = 16.sp,
            modifier = Modifier.width(32.dp),
            fontFamily = FontFamily.Cursive,
            fontWeight = FontWeight.Medium
        )
        AsyncImage(
            model = track.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF282828)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title.split("(").first().trim(),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Cursive,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val durationStr = "%d:%02d".format(track.durationMs / 60000, (track.durationMs % 60000) / 1000)
            Text(
                text = durationStr,
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp,
                fontFamily = FontFamily.Cursive,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color(0xFFB3B3B3))
        }
    }
}
