package com.premium.spotifyclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.premium.spotifyclone.SpotifyCloneApplication
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.data.repository.PlaylistRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSelectionBottomSheet(
    track: Track,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as? SpotifyCloneApplication ?: return
    val playlistRepo = remember(context) { PlaylistRepository(app.database) }
    
    val playlists by playlistRepo.observePlaylists().collectAsState(initial = emptyList())
    
    // Track selected playlists using a set of IDs
    var selectedPlaylistIds by remember { mutableStateOf(setOf<String>()) }
    
    val scope = rememberCoroutineScope()
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Add to playlist",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (playlists.isEmpty()) {
                Text(
                    text = "You don't have any playlists yet.",
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        val isSelected = selectedPlaylistIds.contains(playlist.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPlaylistIds = if (isSelected) {
                                        selectedPlaylistIds - playlist.id
                                    } else {
                                        selectedPlaylistIds + playlist.id
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedPlaylistIds = if (checked) {
                                        selectedPlaylistIds + playlist.id
                                    } else {
                                        selectedPlaylistIds - playlist.id
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF1DB954),
                                    uncheckedColor = Color.Gray,
                                    checkmarkColor = Color.Black
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = playlist.name,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        selectedPlaylistIds.forEach { playlistId ->
                            playlistRepo.addTrack(playlistId, track)
                        }
                        onDismissRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                shape = RoundedCornerShape(24.dp),
                enabled = selectedPlaylistIds.isNotEmpty()
            ) {
                Text(
                    text = "Save",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
