package com.premium.spotifyclone.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.ui.gestures.trackSwipeGesture
import com.premium.spotifyclone.ui.theme.*

@Composable
fun PlayerScreen(
    track: Track,
    isPlaying: Boolean,
    progress: Float,
    isLiked: Boolean = false,
    shuffleEnabled: Boolean = false,
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    onTogglePlay: () -> Unit,
    onCollapse: () -> Unit,
    onSeekToFraction: (Float) -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onSaveTrackClick: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    isLiveSession: Boolean = false,
    onCastClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var draggingProgress by remember { mutableStateOf<Float?>(null) }
    val sliderValue = draggingProgress ?: progress
    val totalMs = track.durationMs.coerceAtLeast(1L)

    var artDrag by remember(track.id) { mutableFloatStateOf(0f) }
    var swipeDirection by remember { mutableIntStateOf(1) }

    val slideTween = remember { tween<IntOffset>(300, easing = FastOutSlowInEasing) }
    val fadeTween  = remember { tween<Float>(240, easing = FastOutSlowInEasing) }

    val repeatOn = repeatMode != Player.REPEAT_MODE_OFF

    var totalDrag by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { totalDrag = 0f },
                    onDragCancel = { totalDrag = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag > 150f) {
                            onCollapse()
                            totalDrag = 0f
                        }
                    }
                )
            }
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // ── Top bar: collapse + "Now Playing" label ─────────────────────────
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCollapse) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = TextPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = "NOW PLAYING",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { /* more options */ }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = IconDefault,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Album art (swipeable) ───────────────────────────────────────────
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
            label = "playerArt"
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val wPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .graphicsLayer { translationX = artDrag }
                        .pointerInput(track.id) {
                            trackSwipeGesture(
                                tapThresholdPx  = with(density) { 24.dp.toPx() },
                                skipThresholdPx = wPx * 0.22f,
                                maxPullPx       = wPx * 0.4f,
                                onDragOffset    = { artDrag = it },
                                onTapExpand     = {},
                                onSkipNext      = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    swipeDirection = 1
                                    onSkipNext()
                                },
                                onSkipPrevious  = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    swipeDirection = -1
                                    onSkipPrevious()
                                },
                                onSwipeDirection = { swipeDirection = it }
                            )
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Title + Artist + Like ───────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (track.isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF282828))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Recommended",
                                color = AccentRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artist,
                    color = TextSecondary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSaveTrackClick()
                },
                enabled = track.audioUrl != null,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    tint = if (isLiked) AccentRed else IconDefault,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Progress Slider ─────────────────────────────────────────────────
        Slider(
            value = sliderValue,
            onValueChange = { draggingProgress = it },
            onValueChangeFinished = {
                draggingProgress?.let { onSeekToFraction(it.coerceIn(0f, 1f)) }
                draggingProgress = null
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor        = TextPrimary,
                activeTrackColor  = TextPrimary,
                inactiveTrackColor = AppElevated
            )
        )

        // Time stamps
        val currentSec = (sliderValue * (totalMs / 1000f)).toInt().coerceAtLeast(0)
        val totalSec   = (totalMs / 1000).toInt().coerceAtLeast(0)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("%d:%02d".format(currentSec / 60, currentSec % 60), color = TextSecondary, fontSize = 12.sp)
            Text("%d:%02d".format(totalSec / 60, totalSec % 60),   color = TextSecondary, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Controls ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(onClick = onToggleShuffle, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleEnabled) AccentRed else IconDefault,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    swipeDirection = -1
                    onSkipPrevious()
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = TextPrimary, modifier = Modifier.size(36.dp))
            }

            // Play / Pause — BIG RED circle
            Box(
                modifier = Modifier
                    .size(68.dp)
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
                    modifier = Modifier.size(36.dp)
                )
            }

            // Next
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    swipeDirection = 1
                    onSkipNext()
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextPrimary, modifier = Modifier.size(36.dp))
            }

            // Repeat
            IconButton(onClick = onCycleRepeat, modifier = Modifier.size(44.dp)) {
                val icon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                Icon(
                    imageVector = icon,
                    contentDescription = "Repeat",
                    tint = if (repeatOn) AccentRed else IconDefault,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Cast (Listen Together)
            IconButton(onClick = onCastClick, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Default.Cast, 
                    contentDescription = "Listen Together", 
                    tint = if (isLiveSession) Color.Red else IconDefault, 
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
