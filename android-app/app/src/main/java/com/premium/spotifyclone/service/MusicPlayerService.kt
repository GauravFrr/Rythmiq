package com.premium.spotifyclone.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.util.Log
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.premium.spotifyclone.MainActivity
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.util.PlaybackPrefs
import com.premium.spotifyclone.data.network.RetrofitInstance
import com.premium.spotifyclone.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URL

@OptIn(UnstableApi::class)
class MusicPlayerService : LifecycleService() {

    private lateinit var musicRepository: MusicRepository

    // ── Single ExoPlayer — shared with bound Activity ─────────────────────────
    lateinit var player: ExoPlayer
        private set

    private lateinit var mediaSessionCompat: MediaSessionCompat // Drives lock screen + MediaStyle transport controls
    private val binder = LocalBinder()

    private var currentAlbumArt: Bitmap? = null
    private var currentTitle:  String = "Premium Spotify"
    private var currentArtist: String = ""
    private var trackDurationMs: Long = 0L
    private val resolvingIds = mutableSetOf<String>() // 🛑 Track IDs currently being resolved

    // ── StateFlows observed by MainActivity ──────────────────────────────────
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue
    
    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex

    private var playQueue: List<Track> = emptyList()
        set(value) {
            field = value
            _queue.value = value
        }

    // Progress ticker job
    private var progressJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "spotify_clone_playback"
        const val ACTION_PLAY_PAUSE = "com.premium.spotifyclone.PLAY_PAUSE"
        const val ACTION_NEXT = "com.premium.spotifyclone.NEXT"
        const val ACTION_PREV = "com.premium.spotifyclone.PREV"
        /** API / UI hint for lock-screen progress when ExoPlayer duration is unset (streams). */
        const val EXTRA_TRACK_DURATION_MS = "track_duration_ms"
        const val EXTRA_TRACK_ID = "track_id"
    }

    // ── Broadcast receiver for notification compact actions (session handles lock screen) ──
    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY_PAUSE -> togglePlayPause()
                ACTION_NEXT       -> skipToNext()
                ACTION_PREV       -> skipToPrevious()
            }
        }
    }

    // Lock screen / Bluetooth / assistant send commands here — must forward to ExoPlayer
    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            player.play()
        }

        override fun onPause() {
            player.pause()
        }

        override fun onSkipToNext() {
            skipToNext()
        }

        override fun onSkipToPrevious() {
            skipToPrevious()
        }

        override fun onSeekTo(pos: Long) {
            player.seekTo(pos)
        }
    }

    // ── Public control called from bound Activity too ─────────────────────────
    fun togglePlayPause() {
        if (!::player.isInitialized) return
        if (effectiveUiPlaying()) player.pause() else player.play()
    }

    fun stopAndClear() {
        if (!::player.isInitialized) return
        player.pause()
        player.clearMediaItems()
        playQueue = emptyList()
        _queue.value = emptyList()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun skipToNext() {
        if (::player.isInitialized && player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                player.prepare()
                player.play()
            }
        }
    }

    fun skipToPrevious() {
        if (!::player.isInitialized) return
        if (player.currentPosition > 3_000L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
            player.prepare()
            player.play()
        }
    }

    fun seekToFraction(fraction: Float) {
        if (!::player.isInitialized) return
        val dur = effectiveDurationMs()
        if (dur <= 0L) return
        val pos = (dur * fraction.toDouble()).toLong().coerceIn(0L, dur)
        player.seekTo(pos)
        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
            player.prepare()
            player.play()
        }
    }

    fun toggleShuffle() {
        if (!::player.isInitialized) return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        if (!::player.isInitialized) return
        val next = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = next
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (!::player.isInitialized) return
        if (fromIndex !in playQueue.indices || toIndex !in 0..playQueue.size) return
        
        val newList = playQueue.toMutableList()
        val item = newList.removeAt(fromIndex)
        // If moving down, toIndex needs adjustment since we removed an item before it
        val actualToIndex = if (toIndex > fromIndex) toIndex - 1 else toIndex
        newList.add(actualToIndex, item)
        playQueue = newList
        
        player.moveMediaItem(fromIndex, toIndex)
    }

    fun removeQueueItem(index: Int) {
        if (!::player.isInitialized) return
        if (index !in playQueue.indices) return
        
        val newList = playQueue.toMutableList()
        newList.removeAt(index)
        playQueue = newList
        
        player.removeMediaItem(index)
    }

    fun addToQueue(track: Track) {
        if (!::player.isInitialized) return
        val currentQueue = playQueue.toMutableList()
        currentQueue.add(track)
        playQueue = currentQueue
        
        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.audioUrl ?: "")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(android.net.Uri.parse(track.coverUrl))
                    .build()
            )
            .build()
        player.addMediaItem(mediaItem)
    }

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        musicRepository = MusicRepository(RetrofitInstance.api)

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.repeatMode = Player.REPEAT_MODE_OFF
        PlaybackPrefs.applyToPlayer(this, player)
        _shuffleEnabled.value = player.shuffleModeEnabled
        _repeatMode.value = player.repeatMode

        mediaSessionCompat = MediaSessionCompat(this, "MusicPlayerService").apply {
            setCallback(mediaSessionCallback)
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }

        // Player listener → sync notification + UI + MediaSession state
        var currentTrackStartTimeMs: Long = 0L
        var accumulatedPlayTimeMs: Long = 0L
        var lastLoggedTrackId: String? = null

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    currentTrackStartTimeMs = System.currentTimeMillis()
                } else if (currentTrackStartTimeMs > 0) {
                    accumulatedPlayTimeMs += (System.currentTimeMillis() - currentTrackStartTimeMs)
                    currentTrackStartTimeMs = 0L
                }
                syncIsPlayingFromPlayer()
                updatePlaybackState()
                updateNotification(currentTitle, currentArtist)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // Keeps UI in sync when playback is driven by playWhenReady without a matching isPlaying edge.
                syncIsPlayingFromPlayer()
            }

            override fun onPlaybackStateChanged(state: Int) {
                // isPlaying alone is not always enough on some devices / timeline edges; mirror ExoPlayer here too.
                syncIsPlayingFromPlayer()
                refreshDurationAndSession()
                
                // If the player hits the end of the queue but we are fetching recommendations (e.g., due to rapid skipping)
                if (state == Player.STATE_ENDED && isFetchingRecommendations) {
                    // It will automatically resume once tracks are added because playWhenReady is true, 
                    // but we can ensure it seeks to the next window and plays when ready once tracks arrive.
                    // We handle the resume logic inside fetchRecommendations callback.
                } else if (state == Player.STATE_ENDED && playQueue.isNotEmpty()) {
                    // Fallback: if it ended and we aren't fetching, try to fetch based on the last track.
                    fetchRecommendations(playQueue.last())
                }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                refreshDurationAndSession()
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                    events.contains(Player.EVENT_TRACKS_CHANGED)
                ) {
                    refreshDurationAndSession()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Log previous track before transitioning
                val previousIndex = _currentQueueIndex.value
                if (previousIndex in playQueue.indices) {
                    val prevTrack = playQueue[previousIndex]
                    if (prevTrack.id != lastLoggedTrackId) {
                        // Calculate final listened duration
                        var totalListened = accumulatedPlayTimeMs
                        if (currentTrackStartTimeMs > 0) {
                            totalListened += (System.currentTimeMillis() - currentTrackStartTimeMs)
                        }
                        
                        // Send log to backend
                        serviceScope.launch {
                            musicRepository.logPlayEvent(
                                com.premium.spotifyclone.data.network.PlayEvent(
                                    songId = prevTrack.id,
                                    songName = prevTrack.title,
                                    artistId = null,
                                    artistName = prevTrack.artist,
                                    albumId = null,
                                    genre = null,
                                    language = null,
                                    duration = prevTrack.durationMs.toInt(),
                                    listenedDuration = totalListened.toInt(),
                                    skipped = reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK,
                                    liked = false,
                                    addedToPlaylist = false,
                                    hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
                                    dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK),
                                    source = "auto_queue"
                                )
                            )
                        }
                        lastLoggedTrackId = prevTrack.id
                    }
                }

                // Reset trackers for the new track
                accumulatedPlayTimeMs = 0L
                if (player.isPlaying) {
                    currentTrackStartTimeMs = System.currentTimeMillis()
                } else {
                    currentTrackStartTimeMs = 0L
                }

                _currentQueueIndex.value = player.currentMediaItemIndex
                applyNowPlaying(player.currentMediaItemIndex)
                
                // Infinite autoplay: fetch more if we are near the end of the queue (buffer of 8)
                val currentIndex = player.currentMediaItemIndex
                if (playQueue.size - currentIndex < 8 && playQueue.isNotEmpty()) {
                    fetchRecommendations(playQueue[currentIndex])
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleEnabled.value = shuffleModeEnabled
                PlaybackPrefs.saveShuffleEnabled(this@MusicPlayerService, shuffleModeEnabled)
                updatePlaybackState()
            }

            override fun onRepeatModeChanged(@Player.RepeatMode repeatMode: Int) {
                _repeatMode.value = repeatMode
                PlaybackPrefs.saveRepeatMode(this@MusicPlayerService, repeatMode)
                updatePlaybackState()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("MusicPlayerService", "ExoPlayer Error: ${error.message}", error)
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                } else if (playQueue.isNotEmpty()) {
                    // We are at the end and got an error. Fetch recommendations to continue!
                    fetchRecommendations(playQueue.last())
                } else {
                    player.stop()
                    player.clearMediaItems()
                }
            }
        })

        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREV)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(controlReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(controlReceiver, filter)
        }

        startForeground(NOTIFICATION_ID, buildNotification(currentTitle, currentArtist, null))
        updatePlaybackState()
    }

    private fun refreshDurationAndSession() {
        val d = player.duration
        if (d != C.TIME_UNSET && d > 0) {
            val merged = maxOf(trackDurationMs, d)
            if (merged != trackDurationMs) {
                trackDurationMs = merged
                updateMediaMetadata()
            }
        }
        updatePlaybackState()
    }

    /** Prefer measured timeline duration; fall back to API hint for streams (helps session + UI). */
    private fun effectiveDurationMs(): Long {
        val pd = if (::player.isInitialized) player.duration else C.TIME_UNSET
        if (pd != C.TIME_UNSET && pd > 0) return pd
        val fromQueue = durationHintFromQueue()
        return maxOf(trackDurationMs, fromQueue).coerceAtLeast(0L)
    }

    private fun durationHintFromQueue(): Long {
        if (!::player.isInitialized || playQueue.isEmpty()) return 0L
        val idx = player.currentMediaItemIndex
        if (idx !in playQueue.indices) return 0L
        return playQueue[idx].durationMs.coerceAtLeast(0L)
    }

    /**
     * ExoPlayer's [Player.isPlaying] is false while buffering even though the user pressed play.
     * UI and the progress ticker follow this "should look like playing" signal instead.
     */
    private fun effectiveUiPlaying(): Boolean {
        if (!::player.isInitialized) return false
        if (player.isPlaying) return true
        if (!player.playWhenReady) return false
        return when (player.playbackState) {
            Player.STATE_BUFFERING,
            Player.STATE_READY -> true
            else -> false
        }
    }

    private fun syncIsPlayingFromPlayer() {
        if (!::player.isInitialized) return
        val playing = effectiveUiPlaying()
        if (_isPlaying.value != playing) {
            _isPlaying.value = playing
            if (playing) startProgressTicker() else stopProgressTicker()
            updatePlaybackState()
        } else if (playing && progressJob?.isActive != true) {
            // Recover if the ticker died while ExoPlayer is still advancing (missed callbacks on some devices).
            startProgressTicker()
        }
    }

    private fun buildMediaItem(url: String, title: String, artist: String, durationHintMs: Long): MediaItem {
        val finalUrl = if (url.startsWith("youtube:")) "http://localhost/searching" else url
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .apply {
                if (durationHintMs > 0) setDurationMs(durationHintMs)
            }
            .build()
        return MediaItem.Builder()
            .setUri(finalUrl)
            .setMediaMetadata(meta)
            .build()
    }

    // ── Start command — new queue or legacy single-track extras ───────────────
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val fromJson = PlaybackQueueJson.fromJson(intent?.getStringExtra(PlaybackQueueJson.EXTRA_QUEUE_JSON))
        val startIndex = intent?.getIntExtra(PlaybackQueueJson.EXTRA_START_INDEX, 0) ?: 0

        playQueue = if (fromJson.isNotEmpty()) {
            fromJson.filter { it.audioUrl != null }
        } else {
            val url = intent?.getStringExtra("audio_url") ?: return START_STICKY
            val title = intent.getStringExtra("track_title") ?: "Playing"
            val artist = intent.getStringExtra("track_artist") ?: ""
            val coverUrl = intent.getStringExtra("track_cover") ?: ""
            val durationHint = intent.getLongExtra(EXTRA_TRACK_DURATION_MS, 0L).coerceAtLeast(0L)
            val id = intent.getStringExtra(EXTRA_TRACK_ID) ?: url.hashCode().toString()
            listOf(Track(id, title, artist, coverUrl, durationHint, url))
        }

        if (playQueue.isEmpty()) return START_STICKY

        val start = startIndex.coerceIn(0, playQueue.lastIndex)
        val items = playQueue.map { t ->
            buildMediaItem(
                t.audioUrl!!,
                t.title,
                t.artist,
                if (t.durationMs > 0) t.durationMs else 0L
            )
        }

        currentAlbumArt = null
        player.setMediaItems(items, start, 0L)
        PlaybackPrefs.applyToPlayer(this, player)
        _shuffleEnabled.value = player.shuffleModeEnabled
        _repeatMode.value = player.repeatMode
        player.prepare()
        player.play()

        applyNowPlaying(start)
        _currentQueueIndex.value = start
        refreshDurationAndSession()
        syncIsPlayingFromPlayer()

        return START_NOT_STICKY
    }

    private fun applyNowPlaying(index: Int) {
        if (playQueue.isEmpty() || index !in playQueue.indices) return
        val t = playQueue[index]
        currentTitle = t.title
        currentArtist = t.artist
        trackDurationMs = if (t.durationMs > 0) t.durationMs else 0L
        currentAlbumArt = null
        _currentTrack.value = t
        updateMediaMetadata()
        updatePlaybackState()
        updateNotification(currentTitle, currentArtist)

        val coverUrl = t.coverUrl
        if (coverUrl.isNotEmpty()) {
            val expectedId = t.id
            serviceScope.launch(Dispatchers.IO) {
                val bmp = loadBitmapFromUrl(coverUrl)
                withContext(Dispatchers.Main) {
                    val still = playQueue.getOrNull(player.currentMediaItemIndex)
                    if (still?.id == expectedId) {
                        currentAlbumArt = bmp
                        updateMediaMetadata()
                        updateNotification(currentTitle, currentArtist)
                    }
                }
            }
        }

        // ── JIT YouTube Stream Resolution ──
        val audioUrl = t.audioUrl ?: ""
        if (audioUrl.startsWith("youtube:")) {
            val searchKey = audioUrl.substringAfter("youtube:")

            // 🛑 IRON GATE: Use track ID (not search key) so different tracks never block each other
            if (resolvingIds.contains(t.id)) {
                android.util.Log.d("MusicPlayerService", "⏳ Already resolving ${t.id}, skipping.")
                return
            }

            android.util.Log.d("MusicPlayerService", "🛠️ JIT: Resolving stream for $searchKey")
            resolvingIds.add(t.id)

            serviceScope.launch {
                try {
                    // ⏱️ 15-second timeout
                    val streamInfo = withTimeoutOrNull(15000) {
                        musicRepository.getStreamUrl(searchKey)
                    }

                    if (streamInfo != null) {
                        android.util.Log.d("MusicPlayerService", "✅ JIT: Got stream URL for $searchKey")
                        withContext(Dispatchers.Main) {
                            val currentIndex = player.currentMediaItemIndex
                            // Only update if the user hasn't skipped to a different track
                            if (currentIndex == index) {
                                // ✅ FIX: Rebuild ALL media items, swapping only the resolved track's URL
                                // This preserves the full queue so next/prev still work.
                                val updatedItems = playQueue.mapIndexed { i, track ->
                                    if (i == index) {
                                        buildMediaItem(streamInfo.streamUrl, track.title, track.artist, track.durationMs)
                                    } else {
                                        buildMediaItem(track.audioUrl ?: "", track.title, track.artist, track.durationMs)
                                    }
                                }
                                player.setMediaItems(updatedItems, currentIndex, player.currentPosition)
                                player.prepare()
                                player.play()
                            }
                        }
                    } else {
                        android.util.Log.e("MusicPlayerService", "❌ JIT: Timed out resolving $searchKey")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MusicPlayerService", "❌ JIT: Failed for $searchKey", e)
                } finally {
                    resolvingIds.remove(t.id) // Unlock using track ID
                }
            }
        }
    }

    // ── Infinite Autoplay / Recommendations ───────────────────────────────────
    private var isFetchingRecommendations = false

    private fun fetchRecommendations(seedTrack: Track) {
        if (isFetchingRecommendations) return
        isFetchingRecommendations = true
        
        serviceScope.launch {
            try {
                // Fetch recommendations using new unified backend endpoint
                val existingIds = playQueue.map { it.id }
                val results = musicRepository.getNextRecommendations(seedTrack.id, existingIds, 10)
                
                val newTracks = results
                    .distinctBy { it.id }
                    .distinctBy { "${it.title.lowercase()}-${it.artist.lowercase()}" }
                    .filter { it.id !in existingIds && !it.audioUrl.isNullOrBlank() }
                    .take(10) // Queue up 10 tracks at a time
                    .map { it.copy(isRecommended = true) }
                
                if (newTracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val updatedQueue = playQueue.toMutableList()
                        updatedQueue.addAll(newTracks)
                        playQueue = updatedQueue
                        
                        val mediaItems = newTracks.map { t ->
                            buildMediaItem(t.audioUrl ?: "", t.title, t.artist, t.durationMs)
                        }
                        player.addMediaItems(mediaItems)
                        
                        // Auto-resume if playback stopped while waiting for network
                        if (player.playbackState == Player.STATE_ENDED || player.playbackState == Player.STATE_IDLE) {
                            // If it errored on the last track, it went to STATE_IDLE or STATE_ENDED. 
                            // Make sure we seek to the new items.
                            if (!player.hasNextMediaItem() && mediaItems.isNotEmpty()) {
                                player.seekTo(player.mediaItemCount - mediaItems.size, 0L)
                            }
                            player.prepare()
                            player.play()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFetchingRecommendations = false
            }
        }
    }

    // ── Progress ticker ───────────────────────────────────────────────────────
    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            var tick = 0
            while (isActive) {
                val durMs = effectiveDurationMs()
                if (effectiveUiPlaying() && durMs > 0) {
                    val pos = player.currentPosition.toFloat()
                    _progress.value = (pos / durMs.toFloat()).coerceIn(0f, 1f)
                    // MediaSession + playback state are relatively heavy; keep UI progress smooth at 500ms
                    // but only sync session position every ~1s to reduce main-thread contention with touches.
                    if (++tick % 2 == 0) updatePlaybackState()
                } else if (effectiveUiPlaying()) {
                    // Duration not in timeline yet — keep bar at start until we can compute a ratio.
                    _progress.value = 0f
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    // ── MediaSession: metadata (title / artist / duration / art) ─────────────
    private fun updateMediaMetadata() {
        val b = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
        if (trackDurationMs > 0) {
            b.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, trackDurationMs)
        }
        currentAlbumArt?.let { b.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it) }
        mediaSessionCompat.setMetadata(b.build())
    }

    // ── MediaSession: playback state (playing/paused + current position) ──────
    // Drives lock-screen position / scrubber when duration metadata is set
    private fun updatePlaybackState() {
        if (!::mediaSessionCompat.isInitialized) return

        val playbackState = when (player.playbackState) {
            Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            Player.STATE_READY -> if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING
            else PlaybackStateCompat.STATE_PAUSED
            Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
            else -> if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING
            else PlaybackStateCompat.STATE_PAUSED
        }
        val speed = when (playbackState) {
            PlaybackStateCompat.STATE_PLAYING -> 1f
            PlaybackStateCompat.STATE_BUFFERING -> 0f
            else -> 0f
        }
        val pos = if (::player.isInitialized) player.currentPosition else 0L

        val pbState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setBufferedPosition(if (::player.isInitialized) player.bufferedPosition else 0L)
            .setState(playbackState, pos, speed, SystemClock.elapsedRealtime())
            .build()

        mediaSessionCompat.setPlaybackState(pbState)
    }

    // ── Notification builder ──────────────────────────────────────────────────
    private fun updateNotification(title: String, artist: String) {
        val notification = buildNotification(title, artist, currentAlbumArt)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, artist: String, albumArt: Bitmap?): Notification {
        val openAppPi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // ✅ setPackage(packageName) ensures RECEIVER_NOT_EXPORTED receivers get the broadcast
        fun broadcastPi(requestCode: Int, action: String) = PendingIntent.getBroadcast(
            this, requestCode,
            Intent(action).setPackage(packageName),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevPi      = broadcastPi(1, ACTION_PREV)
        val playPausePi = broadcastPi(2, ACTION_PLAY_PAUSE)
        val nextPi      = broadcastPi(3, ACTION_NEXT)

        val isPlayingNow = if (::player.isInitialized) effectiveUiPlaying() else false
        val playPauseIcon = if (isPlayingNow) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        val playPauseLabel = if (isPlayingNow) "Pause" else "Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(albumArt)
            .setContentIntent(openAppPi)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPi)
            .addAction(playPauseIcon, playPauseLabel, playPausePi)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPi)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSessionCompat.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlayingNow)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun loadBitmapFromUrl(urlString: String): Bitmap? = try {
        val conn = URL(urlString).openConnection()
        conn.connect()
        BitmapFactory.decodeStream(conn.getInputStream())
    } catch (e: Exception) { null }

    private fun createNotificationChannel() {
        // IMPORTANCE_LOW can hide or limit media controls on some devices; use DEFAULT for playback
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music Playback",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows currently playing track"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        if (playQueue.isNotEmpty() && ::player.isInitialized) {
            applyNowPlaying(player.currentMediaItemIndex.coerceIn(0, playQueue.lastIndex))
        }
        syncIsPlayingFromPlayer()
        return binder
    }

    override fun onDestroy() {
        stopProgressTicker()
        unregisterReceiver(controlReceiver)
        mediaSessionCompat.setCallback(null)
        mediaSessionCompat.release()
        player.release()
        super.onDestroy()
    }
}
