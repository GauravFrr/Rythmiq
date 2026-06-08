package com.premium.spotifyclone.ui.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

/**
 * One pointer: tap (open player) vs dominant-horizontal swipe (skip).
 * Ignores dominant-vertical movement so sloppy vertical drags do not open the player.
 */
suspend fun PointerInputScope.trackSwipeGesture(
    tapThresholdPx: Float,
    skipThresholdPx: Float,
    maxPullPx: Float,
    onDragOffset: (Float) -> Unit,
    onTapExpand: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSwipeDirection: (Int) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var totalX = 0f
        var totalY = 0f
        onDragOffset(0f)

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change: PointerInputChange = event.changes.firstOrNull { it.id == down.id } ?: continue

            if (change.changedToUpIgnoreConsumed()) {
                onDragOffset(0f)
                val ax = abs(totalX)
                val ay = abs(totalY)
                when {
                    ax < tapThresholdPx && ay < tapThresholdPx -> {
                        if (!change.isConsumed) {
                            onSwipeDirection(0)
                            onTapExpand()
                        }
                    }
                    ax > ay && totalX < -skipThresholdPx -> {
                        onSwipeDirection(1)
                        onSkipPrevious()
                    }
                    ax > ay && totalX > skipThresholdPx -> {
                        onSwipeDirection(-1)
                        onSkipNext()
                    }
                }
                break
            }

            val dx = change.positionChange().x
            val dy = change.positionChange().y
            totalX += dx
            totalY += dy
            if (abs(totalX) >= abs(totalY)) {
                onDragOffset(totalX.coerceIn(-maxPullPx, maxPullPx))
            } else {
                onDragOffset(0f)
            }
            change.consume()
        }
    }
}
