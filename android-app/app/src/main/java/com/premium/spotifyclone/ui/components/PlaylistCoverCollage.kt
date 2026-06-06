package com.premium.spotifyclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.premium.spotifyclone.data.models.Track

@Composable
fun PlaylistCoverCollage(
    coverUrls: List<String?>,
    modifier: Modifier = Modifier,
) {
    val gap = 2.dp
    val gapColor = Color(0xFF121212)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A2A)),
    ) {
        when (coverUrls.size) {
            0 -> Unit
            1 -> CollageImageCell(url = coverUrls[0], modifier = Modifier.fillMaxSize())
            2 -> Row(modifier = Modifier.fillMaxSize()) {
                CollageImageCell(url = coverUrls[0], modifier = Modifier.weight(1f).fillMaxHeight())
                Box(
                    modifier = Modifier
                        .width(gap)
                        .fillMaxHeight()
                        .background(gapColor),
                )
                CollageImageCell(url = coverUrls[1], modifier = Modifier.weight(1f).fillMaxHeight())
            }
            3 -> Row(modifier = Modifier.fillMaxSize()) {
                CollageImageCell(url = coverUrls[0], modifier = Modifier.weight(1f).fillMaxHeight())
                Box(
                    modifier = Modifier
                        .width(gap)
                        .fillMaxHeight()
                        .background(gapColor),
                )
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CollageImageCell(url = coverUrls[1], modifier = Modifier.weight(1f).fillMaxWidth())
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gap)
                            .background(gapColor),
                    )
                    CollageImageCell(url = coverUrls[2], modifier = Modifier.weight(1f).fillMaxWidth())
                }
            }
            else -> Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageImageCell(url = coverUrls[0], modifier = Modifier.weight(1f).fillMaxHeight())
                    Box(
                        modifier = Modifier
                            .width(gap)
                            .fillMaxHeight()
                            .background(gapColor),
                    )
                    CollageImageCell(url = coverUrls[1], modifier = Modifier.weight(1f).fillMaxHeight())
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gap)
                        .background(gapColor),
                )
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageImageCell(url = coverUrls[2], modifier = Modifier.weight(1f).fillMaxHeight())
                    Box(
                        modifier = Modifier
                            .width(gap)
                            .fillMaxHeight()
                            .background(gapColor),
                    )
                    CollageImageCell(url = coverUrls[3], modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
fun CollageImageCell(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color(0xFF2A2A2A)),
    ) {
        val u = url?.takeIf { it.isNotBlank() }
        if (u != null) {
            AsyncImage(
                model = u,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun PlaylistStyleActionRow(
    thumbCoverUrls: List<String?>,
    playableTracks: List<Track>,
    shuffleEnabled: Boolean,
    onPlayPlaylist: () -> Unit,
    onToggleShuffle: () -> Unit,
) {
    val spotifyGreen = Color(0xFF1DB954)
    var moreMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF333333)),
        ) {
            PlaylistCoverCollage(
                coverUrls = thumbCoverUrls.take(4),
                modifier = Modifier.fillMaxSize(),
            )
        }
        IconButton(onClick = { /* offline download not implemented */ }) {
            Icon(
                Icons.Default.Download,
                contentDescription = "Download",
                tint = Color.White,
            )
        }
        IconButton(onClick = { /* share not implemented */ }) {
            Icon(
                Icons.Default.Share,
                contentDescription = "Share",
                tint = Color.White,
            )
        }
        Box {
            IconButton(onClick = { moreMenuOpen = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.White,
                )
            }
            DropdownMenu(
                expanded = moreMenuOpen,
                onDismissRequest = { moreMenuOpen = false },
                containerColor = Color(0xFF2A2A2A),
            ) {
                DropdownMenuItem(
                    text = { Text("Playlist menu", color = Color(0xFF888888)) },
                    onClick = { moreMenuOpen = false },
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onToggleShuffle) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = if (shuffleEnabled) "Shuffle on" else "Shuffle off",
                tint = if (shuffleEnabled) spotifyGreen else Color.White,
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 4.dp, end = 4.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(spotifyGreen)
                .clickable(enabled = playableTracks.isNotEmpty(), onClick = onPlayPlaylist),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play playlist",
                tint = Color.Black,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}
