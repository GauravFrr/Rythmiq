package com.premium.spotifyclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistScreen(
    onNavigateBack: () -> Unit,
    onSavePlaylist: (id: String, name: String) -> Unit,
    isCollaborative: Boolean = false
) {
    var playlistName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                if (isCollaborative) "Create Collaborative Playlist" else "Create Playlist",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Input Field
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Name your playlist",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            TextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color(0xFF1DB954),
                    cursorColor = Color(0xFF1DB954)
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                placeholder = {
                    Text(
                        "My Playlist",
                        color = Color(0xFF535353),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Create Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (playlistName.isNotBlank()) Color(0xFF1DB954) else Color(0xFF535353))
                    .clickable(enabled = playlistName.isNotBlank()) {
                        val newId = "playlist_${UUID.randomUUID()}"
                        onSavePlaylist(newId, playlistName.trim())
                        onNavigateBack()
                    }
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Create",
                    color = if (playlistName.isNotBlank()) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
