package com.premium.spotifyclone.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreateBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onCreateCollaborative: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(350)
        ),
        exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Prevent click-through
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF282828),
                                Color(0xFF1E1E1E)
                            )
                        )
                    )
                    .padding(top = 8.dp, bottom = 16.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF535353))
                )

                Spacer(modifier = Modifier.height(20.dp))

                CreateOption(
                    icon = Icons.Default.MusicNote,
                    title = "Playlist",
                    subtitle = "Create a playlist with songs or episodes",
                    iconBgColor = Color(0xFF2D2D4E),
                    onClick = {
                        onDismiss()
                        onCreatePlaylist()
                    }
                )

                CreateOption(
                    icon = Icons.Default.Group,
                    title = "Collaborative playlist",
                    subtitle = "Create a playlist together with friends",
                    iconBgColor = Color(0xFF2D3E2D),
                    onClick = {
                        onDismiss()
                        onCreateCollaborative()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Close button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF404040))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBgColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                subtitle,
                color = Color(0xFFB3B3B3),
                fontSize = 13.sp
            )
        }
    }
}
