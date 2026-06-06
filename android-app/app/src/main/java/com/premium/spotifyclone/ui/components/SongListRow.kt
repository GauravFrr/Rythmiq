package com.premium.spotifyclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.ui.theme.AccentRed
import com.premium.spotifyclone.ui.theme.AppDivider
import com.premium.spotifyclone.ui.theme.AppSurface
import com.premium.spotifyclone.ui.theme.IconDefault
import com.premium.spotifyclone.ui.theme.TextPrimary
import com.premium.spotifyclone.ui.theme.TextSecondary

/**
 * Rythmiq standard song list row.
 * [ 52dp art ] [ Title / Artist ] [ ♡ ] [ 3:19 ] [ ⋮ ]
 */
@Composable
fun SongListRow(
    track: Track,
    isLiked: Boolean = false,
    showDivider: Boolean = true,
    onClick: () -> Unit,
    onLikeClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    numberPrefix: Int? = null       // optional track number (for Charts)
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional track number prefix
            if (numberPrefix != null) {
                Text(
                    text = numberPrefix.toString(),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(28.dp)
                )
            }

            // Album art
            AsyncImage(
                model = track.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppSurface),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title + Artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Like button
            if (onLikeClick != null) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) AccentRed else IconDefault,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Duration
            val durationSec = (track.durationMs / 1000).toInt()
            if (durationSec > 0) {
                Text(
                    text = "%d:%02d".format(durationSec / 60, durationSec % 60),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Three-dot menu
            if (onMoreClick != null) {
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = IconDefault,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Divider
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .padding(start = 80.dp) // aligns with text (skips art)
                    .background(AppDivider)
            )
        }
    }
}
