package com.premium.spotifyclone.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
class AudioPlayerManager(context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress
    
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })

        // Simple progress ticker
        scope.launch {
            while (true) {
                if (player.isPlaying && player.duration > 0) {
                    _progress.value = player.currentPosition.toFloat() / player.duration.toFloat()
                }
                delay(1000L) // Update every second
            }
        }
    }

    fun playTrack(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.play()
    }

    fun release() {
        player.release()
    }
}
