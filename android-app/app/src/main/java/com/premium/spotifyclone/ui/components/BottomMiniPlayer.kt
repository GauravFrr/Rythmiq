package com.premium.spotifyclone.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.premium.spotifyclone.ui.utils.extractDominantColor
import kotlinx.coroutines.isActive

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

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var dragDuring by remember { mutableFloatStateOf(0f) }
    var swipeDirection by remember { mutableIntStateOf(1) }

    val slideTween = remember { tween<IntOffset>(280, easing = FastOutSlowInEasing) }
    val fadeTween  = remember { tween<Float>(220, easing = FastOutSlowInEasing) }

    // Dynamic Color Palette
    var dominantColor by remember { mutableStateOf(Color(0xFF181818)) }
    val animatedBgColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800),
        label = "miniPlayerBgColor"
    )

    LaunchedEffect(track.coverUrl) {
        val color = extractDominantColor(context, track.coverUrl)
        if (color != null) {
            dominantColor = color
        } else {
            dominantColor = Color(0xFF181818)
        }
    }

    // Bluetooth Headset Detection
    var isBluetoothHeadsetConnected by remember { mutableStateOf(false) }
    LaunchedEffect(track.id) {
        while (isActive) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            var isBt = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                isBt = devices.any { 
                    it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
                    it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                }
            } else {
                @Suppress("DEPRECATION")
                isBt = audioManager.isBluetoothA2dpOn
            }
            isBluetoothHeadsetConnected = isBt
            kotlinx.coroutines.delay(3000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(animatedBgColor)
            .clickable { onExpand() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album Art
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = "Cover",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppBlack),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & Artist
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track.title,
                                modifier = Modifier.weight(1f, fill = false),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isBluetoothHeadsetConnected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = "Bluetooth Connected",
                                    tint = Color.Red,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            color = Color(0xFFB3B3B3),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Action Icons (Like, Add to Queue, Play/Pause)
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSaveTrackClick()
                            },
                            enabled = track.audioUrl != null,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) AccentRed else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAddToQueueClick()
                            }, 
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd, 
                                contentDescription = "Add to Queue", 
                                tint = Color.White, 
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTogglePlay()
                            }, 
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Syncing Progress Bar (Absolute Bottom Edge)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            ) {
                val trackHeight = 2.dp.toPx()
                val progressWidth = size.width * progress.coerceIn(0f, 1f)
                
                // Background track
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = trackHeight
                )
                
                // Progress track
                if (progressWidth > 0) {
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                        end = androidx.compose.ui.geometry.Offset(progressWidth, size.height / 2f),
                        strokeWidth = trackHeight
                    )
                }
            }
        }
    }
}
