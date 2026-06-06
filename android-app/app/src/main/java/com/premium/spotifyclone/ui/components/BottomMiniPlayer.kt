package com.premium.spotifyclone.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import coil.compose.AsyncImage
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.ui.gestures.trackSwipeGesture
import com.premium.spotifyclone.ui.theme.*

@Composable
fun BottomMiniPlayer(
    track: Track?,
    isPlaying: Boolean,
    progress: Float,
    isLiked: Boolean = false,
    onTogglePlay: () -> Unit,
    onExpand: () -> Unit,
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onSaveTrackClick: () -> Unit = {},
    onAddToQueueClick: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    isLiveSession: Boolean = false,
    onCastClick: () -> Unit = {}
) {
    if (track == null) return

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var dragDuring by remember { mutableFloatStateOf(0f) }
    var swipeDirection by remember { mutableIntStateOf(1) }

    val slideTween = remember { tween<IntOffset>(280, easing = FastOutSlowInEasing) }
    val fadeTween  = remember { tween<Float>(220, easing = FastOutSlowInEasing) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF181818).copy(alpha = 0.95f)) // Perfect dark translucent
            .clickable { onExpand() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ── Row 1: Art + Title/Artist + Action Icons ───────────────────────────────
            AnimatedContent(
                targetState = track.id,
                transitionSpec = {
                    when {
                        swipeDirection == 0 -> fadeIn(fadeTween) togetherWith fadeOut(fadeTween)
                        swipeDirection > 0  ->
                            (slideInHorizontally(slideTween) { it } + fadeIn(fadeTween))
                                .togetherWith(slideOutHorizontally(slideTween) { -it } + fadeOut(fadeTween))
                        else ->
                            (slideInHorizontally(slideTween) { -it } + fadeIn(fadeTween))
                                .togetherWith(slideOutHorizontally(slideTween) { it } + fadeOut(fadeTween))
                    }
                },
                label = "miniPlayerTrack"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = dragDuring }
                        .pointerInput(track.id) {
                            val widthPx = with(density) { size.width.toFloat() }
                            trackSwipeGesture(
                                tapThresholdPx     = with(density) { 22.dp.toPx() },
                                skipThresholdPx    = widthPx * 0.28f,
                                maxPullPx          = widthPx * 0.45f,
                                onDragOffset       = { dragDuring = it },
                                onTapExpand        = onExpand,
                                onSkipNext         = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSkipNext()
                                },
                                onSkipPrevious     = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSkipPrevious()
                                },
                                onSwipeDirection   = { swipeDirection = it }
                            )
                        }
                        .padding(start = 12.dp, top = 12.dp, end = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album art
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = "Cover",
                        modifier = Modifier
                            .size(52.dp) // Slightly larger to match reference
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppBlack),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title + Artist
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track.title,
                                modifier = Modifier.weight(1f, fill = false),
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (track.isRecommended) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF282828))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Recommended",
                                        color = AccentRed, // Using accent color for visibility
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Top Right Icons (Heart, Mic, Add to Queue, View Queue)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSaveTrackClick()
                            },
                            enabled = track.audioUrl != null,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) AccentRed else IconDefault,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Mic, contentDescription = "Lyrics", tint = IconDefault, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddToQueueClick()
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to Queue", tint = IconDefault, modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenQueue()
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.FormatListBulleted, contentDescription = "Queue", tint = IconDefault, modifier = Modifier.size(20.dp))
                        }
                        if (isLiveSession) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }

            // ── Row 2: Progress Bar with Timers ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalMs = track.durationMs.coerceAtLeast(1L)
                val currentSec = (progress * (totalMs / 1000f)).toInt().coerceAtLeast(0)
                val totalSec   = (totalMs / 1000).toInt().coerceAtLeast(0)

                Text("%d:%02d".format(currentSec / 60, currentSec % 60), color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clickable { onExpand() }
                ) {
                    val trackHeight = 1.dp.toPx()
                    val centerY = size.height / 2f
                    
                    // Background track (dark grey)
                    drawLine(
                        color = Color(0xFF333333),
                        start = androidx.compose.ui.geometry.Offset(0f, centerY),
                        end = androidx.compose.ui.geometry.Offset(size.width, centerY),
                        strokeWidth = trackHeight
                    )
                    
                    // Progress track (light grey/white, not red!)
                    val progressWidth = size.width * progress.coerceIn(0f, 1f)
                    if (progressWidth > 0) {
                        drawLine(
                            color = Color(0xFFCCCCCC),
                            start = androidx.compose.ui.geometry.Offset(0f, centerY),
                            end = androidx.compose.ui.geometry.Offset(progressWidth, centerY),
                            strokeWidth = trackHeight
                        )
                        // Square dot at the end to match screenshot precisely
                        val thumbSize = 5.dp.toPx()
                        drawRect(
                            color = Color(0xFFCCCCCC),
                            topLeft = androidx.compose.ui.geometry.Offset(
                                x = progressWidth - (thumbSize / 2f),
                                y = centerY - (thumbSize / 2f)
                            ),
                            size = androidx.compose.ui.geometry.Size(thumbSize, thumbSize)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("%d:%02d".format(totalSec / 60, totalSec % 60), color = TextSecondary, fontSize = 11.sp)
            }

            // ── Row 3: Playback controls ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .padding(bottom = 8.dp)
            ) {
                val sidePadding = 34.dp // Padding away from the center Play button
                
                // Left side: Shuffle, Share, Previous
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(0.5f)
                        .padding(end = sidePadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = IconDefault, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = IconDefault, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSkipPrevious()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = TextPrimary, modifier = Modifier.size(24.dp))
                    }
                }

                // Center: Play Button (Guaranteed absolute center)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(AccentRed)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTogglePlay()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Right side: Next, Repeat, Cast
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(0.5f)
                        .padding(start = sidePadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSkipNext()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextPrimary, modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = IconDefault, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onCastClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Cast, contentDescription = "Listen Together", tint = if (isLiveSession) Color.Red else IconDefault, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
