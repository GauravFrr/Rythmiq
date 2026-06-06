package com.premium.spotifyclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.premium.spotifyclone.data.models.Track
import kotlin.math.abs

@Composable
fun QueueScreen(
    queue: List<Track>,
    currentQueueIndex: Int,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClose: () -> Unit
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var draggedOffsetY by remember { mutableStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 72.dp.toPx() } // Approximate height of each row

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .systemBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Playing Queue",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Cursive
            )
        }

        if (queue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Queue is empty", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                itemsIndexed(queue) { index, track ->
                    val isPlaying = index == currentQueueIndex
                    val isDragged = index == draggedIndex
                    
                    val translationY = if (isDragged) draggedOffsetY else 0f
                    val zIndex = if (isDragged) 1f else 0f
                    val scale = if (isDragged) 1.05f else 1f

                    // Show "Autoplay Next" header before the first recommended track
                    if (track.isRecommended && (index == 0 || !queue[index - 1].isRecommended)) {
                        Text(
                            text = "Autoplay Next",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .graphicsLayer {
                                this.translationY = translationY
                                this.scaleX = scale
                                this.scaleY = scale
                            }
                            .zIndex(zIndex)
                            .background(if (isDragged) Color(0xFF282828) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.coverUrl,
                            contentDescription = "Cover",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF282828)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = if (isPlaying) Color(0xFF1DB954) else Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                color = Color(0xFFAAAAAA),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        // Remove Button
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray)
                        }

                        // Drag Handle
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Reorder",
                            tint = Color.Gray,
                            modifier = Modifier
                                .padding(8.dp)
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedIndex = index
                                            draggedOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggedOffsetY += dragAmount.y
                                            
                                            val direction = if (draggedOffsetY > 0) 1 else -1
                                            if (abs(draggedOffsetY) > itemHeightPx * 0.6f) {
                                                val targetIndex = draggedIndex!! + direction
                                                if (targetIndex in queue.indices) {
                                                    onMove(draggedIndex!!, targetIndex)
                                                    draggedIndex = targetIndex
                                                    draggedOffsetY -= direction * itemHeightPx
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            draggedOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            draggedOffsetY = 0f
                                        }
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}
