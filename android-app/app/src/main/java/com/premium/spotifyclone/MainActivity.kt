package com.premium.spotifyclone

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.data.repository.LikedTracksRepository
import com.premium.spotifyclone.data.repository.PlaylistRepository
import com.premium.spotifyclone.service.MusicPlayerService
import com.premium.spotifyclone.service.PlaybackQueueJson
import com.premium.spotifyclone.ui.components.BottomMiniPlayer
import com.premium.spotifyclone.ui.components.CreateBottomSheet
import com.premium.spotifyclone.ui.components.ProfileDrawer
import com.premium.spotifyclone.ui.screens.PlayerScreen
import com.premium.spotifyclone.ui.screens.QueueScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import com.premium.spotifyclone.ui.theme.SpotifyCloneTheme
import com.premium.spotifyclone.util.LastPlayedPrefs
import com.premium.spotifyclone.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import android.util.Log

class MainActivity : ComponentActivity() {

    private var musicService: MusicPlayerService? = null
    private var isBound = false
    private var serviceStarted = false
    private var serviceCollectJob: Job? = null

    private val activeTrackHold = MutableStateFlow<Track?>(null)
    private val uiIsPlaying = MutableStateFlow(false)
    private val uiProgress = MutableStateFlow(0f)
    private val uiShuffleEnabled = MutableStateFlow(false)
    private val uiRepeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    private val uiQueue = MutableStateFlow<List<Track>>(emptyList())
    private val uiCurrentQueueIndex = MutableStateFlow(0)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as MusicPlayerService.LocalBinder).getService()
            musicService = service
            isBound = true
            uiIsPlaying.value = service.isPlaying.value
            uiProgress.value = service.progress.value
            uiShuffleEnabled.value = service.shuffleEnabled.value
            uiRepeatMode.value = service.repeatMode.value
            serviceCollectJob?.cancel()
            serviceCollectJob = lifecycleScope.launch {
                launch { service.isPlaying.collect { uiIsPlaying.value = it } }
                launch { service.progress.collect { uiProgress.value = it } }
                launch { service.shuffleEnabled.collect { uiShuffleEnabled.value = it } }
                launch { service.repeatMode.collect { uiRepeatMode.value = it } }
                launch { service.queue.collect { uiQueue.value = it } }
                launch { service.currentQueueIndex.collect { uiCurrentQueueIndex.value = it } }
                launch {
                    service.currentTrack.collect { t ->
                        if (t != null) {
                            activeTrackHold.value = t
                            LastPlayedPrefs.save(this@MainActivity, t)
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceCollectJob?.cancel()
            serviceCollectJob = null
            musicService = null
            isBound = false
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* nothing extra needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (activeTrackHold.value == null) {
            activeTrackHold.value = LastPlayedPrefs.load(this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val context = LocalContext.current
            val app = context.applicationContext as? SpotifyCloneApplication ?: return@setContent
            val likedRepo = remember { LikedTracksRepository(app.database) }
            val playlistRepository = remember { PlaylistRepository(app.database) }
            val playlists by playlistRepository.observePlaylists()
                .collectAsStateWithLifecycle(emptyList())
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }
            var saveSheetOpen by remember { mutableStateOf(false) }
            var isCreateSheetOpen by remember { mutableStateOf(false) }
            var isProfileDrawerOpen by remember { mutableStateOf(false) }
            var isListenTogetherSheetOpen by remember { mutableStateOf(false) }

            val sessionViewModel: com.premium.spotifyclone.viewmodel.SessionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val currentRoom by sessionViewModel.currentRoom.collectAsStateWithLifecycle()
            val isLiveSession = currentRoom != null
            
            val navController = androidx.navigation.compose.rememberNavController()
            val activeTrack by activeTrackHold.collectAsState()
            val isPlaying by uiIsPlaying.collectAsStateWithLifecycle()
            val progress by uiProgress.collectAsStateWithLifecycle()
            val shuffleEnabled by uiShuffleEnabled.collectAsState()
            val repeatMode by uiRepeatMode.collectAsState()
            val isLikedFromRepo by likedRepo.observeIsLiked(activeTrack?.id ?: "")
                .collectAsStateWithLifecycle(initialValue = false)
            var likeUiOverride by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(activeTrack?.id) {
                likeUiOverride = null
                saveSheetOpen = false
                isCreateSheetOpen = false
                isProfileDrawerOpen = false
            }
            LaunchedEffect(isLikedFromRepo, likeUiOverride) {
                if (likeUiOverride != null && likeUiOverride == isLikedFromRepo) {
                    likeUiOverride = null
                }
            }
            val isLiked = likeUiOverride ?: isLikedFromRepo
            var isPlayerExpanded by remember { mutableStateOf(false) }
            var isQueueExpanded by remember { mutableStateOf(false) }
            val queue by uiQueue.collectAsStateWithLifecycle()
            val currentQueueIndex by uiCurrentQueueIndex.collectAsStateWithLifecycle()

            // Observe the auth token for personalized recommendations
            val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

            // Auth state for showing full-screen flow
            var loginStartWith by remember { mutableStateOf("") } // "email" | "phone" | "google"

            val onSaveTrackClick: () -> Unit = save@{
                val t = activeTrack ?: return@save
                if (t.audioUrl == null) return@save
                if (isLiked) {
                    likeUiOverride = false
                    scope.launch {
                        try {
                            likedRepo.setLiked(t, false)
                            snackbarHostState.showSnackbar(
                                message = "Removed from liked songs",
                                duration = SnackbarDuration.Short,
                            )
                        } catch (_: Exception) {
                            likeUiOverride = null
                        }
                    }
                } else {
                    likeUiOverride = true
                    scope.launch {
                        try {
                            likedRepo.setLiked(t, true)
                            snackbarHostState.showSnackbar(
                                message = "Song added to liked",
                                duration = SnackbarDuration.Short,
                            )
                        } catch (_: Exception) {
                            likeUiOverride = null
                        }
                    }
                }
            }

            var isInitiator by remember { mutableStateOf(false) }

            val onPlayTracksRemote: (List<Track>) -> Unit = onPlayTracksRemote@{ tracks ->
                val withAudio = tracks.filter { it.audioUrl != null }
                if (withAudio.isEmpty()) return@onPlayTracksRemote
                activeTrackHold.value = withAudio.first()
                LastPlayedPrefs.save(this@MainActivity, withAudio.first())
                val intent = Intent(this@MainActivity, MusicPlayerService::class.java).apply {
                    putExtra(PlaybackQueueJson.EXTRA_QUEUE_JSON, PlaybackQueueJson.toJson(withAudio))
                    putExtra(PlaybackQueueJson.EXTRA_START_INDEX, 0)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                serviceStarted = true
                bindToService()
            }

            val onPlayTracks: (List<Track>) -> Unit = onPlayTracks@{ tracks ->
                isInitiator = true
                val withAudio = tracks.filter { it.audioUrl != null }
                if (withAudio.isEmpty()) return@onPlayTracks
                
                onPlayTracksRemote(tracks)
            }

            LaunchedEffect(Unit) {
                launch {
                    sessionViewModel.syncSong.collect { track ->
                        isInitiator = false
                        onPlayTracksRemote(listOf(track))
                    }
                }
                launch {
                    sessionViewModel.syncPlayPause.collect { (playing, timestamp) ->
                        isInitiator = false
                        musicService?.setPlayPause(playing)
                        if (timestamp > 0) {
                            musicService?.seekToMs(timestamp)
                        }
                    }
                }
                launch {
                    sessionViewModel.syncSeek.collect { positionMs ->
                        isInitiator = false
                        val current = musicService?.player?.currentPosition ?: 0L
                        if (kotlin.math.abs(current - positionMs) > 1500) {
                            musicService?.seekToMs(positionMs)
                        }
                    }
                }
                launch {
                    sessionViewModel.syncQueue.collect { track ->
                        musicService?.addToQueue(track)
                    }
                }
            }

            LaunchedEffect(activeTrack) {
                if (isInitiator && isLiveSession && activeTrack != null) {
                    sessionViewModel.syncSong(activeTrack!!)
                }
            }

            LaunchedEffect(isPlaying, isInitiator, isLiveSession) {
                if (isPlaying && isInitiator && isLiveSession) {
                    while (true) {
                        kotlinx.coroutines.delay(3000)
                        val pos = musicService?.player?.currentPosition ?: 0L
                        sessionViewModel.syncSeek(pos)
                    }
                }
            }

            SpotifyCloneTheme {
                Box(modifier = Modifier.fillMaxSize()) {

                    // ── LOGIN GATE ─────────────────────────────────────────────────────────
                    if (!isLoggedIn) {
                        if (loginStartWith.isEmpty()) {
                            com.premium.spotifyclone.ui.screens.OnboardingScreen(
                                onContinueWithEmail = { loginStartWith = "email" },
                                onContinueWithGoogle = {
                                    scope.launch {
                                        try {
                                            val credentialManager = CredentialManager.create(this@MainActivity)
                                            val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                                                .setFilterByAuthorizedAccounts(false)
                                                .setServerClientId("965773273981-hj4lkbd34h1tt8lcuie6t7afiu0vc3u1.apps.googleusercontent.com")
                                                .setAutoSelectEnabled(false)
                                                .build()

                                            val request = GetCredentialRequest.Builder()
                                                .addCredentialOption(googleIdOption)
                                                .build()

                                            val result = credentialManager.getCredential(
                                                request = request,
                                                context = this@MainActivity
                                            )
                                            val credential = result.credential
                                            if (credential is androidx.credentials.CustomCredential &&
                                                credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                            ) {
                                                val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                                                val token = googleIdTokenCredential.idToken
                                                authViewModel.pendingGoogleToken = token
                                                loginStartWith = "google"
                                                authViewModel.loginWithGoogleToken(
                                                    idToken = token,
                                                    displayName = googleIdTokenCredential.displayName,
                                                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                                                )
                                            }
                                        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                                            android.util.Log.e("Auth", "Google sign-in failed", e)
                                            android.widget.Toast.makeText(
                                                this@MainActivity,
                                                "Google Sign-In unavailable on this emulator. Please use Email or Phone.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                onContinueWithPhone = { loginStartWith = "phone" },
                                onSignUp = { loginStartWith = "email_signup" }
                            )
                        } else {
                            com.premium.spotifyclone.ui.screens.auth.AuthNavGraph(
                                initialRoute = loginStartWith,
                                authViewModel = authViewModel,
                                onAuthSuccess = { loginStartWith = "" },
                                onBackToOnboarding = { loginStartWith = "" }
                            )
                        }
                        return@SpotifyCloneTheme
                    }

                    // ── MAIN APP (only shown when logged in) ───────────────────────────────
                    Scaffold(
                        topBar = {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            
                            // Only show top tabs on main destinations
                            if (currentRoute in listOf("home_tab", "hot_and_new_tab", "charts_tab", "library_tab")) {
                                com.premium.spotifyclone.ui.components.RythmiqTopBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { route ->
                                        navController.navigate(route) {
                                            popUpTo("home_tab") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onOpenDrawer = { isProfileDrawerOpen = true },
                                    onSearchClick = { navController.navigate(com.premium.spotifyclone.ui.navigation.Screen.Search.route) }
                                )
                            }
                        },
                        bottomBar = {
                            // BottomBar removed to allow content to scroll behind the floating player
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            com.premium.spotifyclone.ui.navigation.SpotifyNavGraph(
                                navController = navController,
                                modifier = Modifier
                                    .padding(top = innerPadding.calculateTopPadding(), bottom = if (activeTrack != null) 64.dp else 0.dp)
                                    .fillMaxSize(),
                                onPlayTracks = onPlayTracks,
                                onAddToQueue = { track ->
                                    musicService?.addToQueue(track)
                                    if (isLiveSession) {
                                        sessionViewModel.syncQueue(track)
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Added to queue",
                                            duration = SnackbarDuration.Short,
                                        )
                                    }
                                },
                                shuffleEnabled = shuffleEnabled,
                                onToggleShuffle = { musicService?.toggleShuffle() },
                                onOpenDrawer = { isProfileDrawerOpen = true }
                            )
                            
                            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                                BottomMiniPlayer(
                                    track = activeTrack,
                                    isPlaying = isPlaying,
                                    progress = progress,
                                    isLiked = isLiked,
                                    onTogglePlay = { 
                                        isInitiator = true
                                        musicService?.togglePlayPause()
                                        if (isLiveSession) {
                                            sessionViewModel.syncPlayPause(!isPlaying, musicService?.player?.currentPosition ?: 0L)
                                        }
                                    },
                                    onExpand = { isPlayerExpanded = true },
                                    onSkipNext = { 
                                        isInitiator = true
                                        musicService?.skipToNext() 
                                    },
                                    onSkipPrevious = { 
                                        isInitiator = true
                                        musicService?.skipToPrevious() 
                                    },
                                    onSaveTrackClick = onSaveTrackClick,
                                    onAddToQueueClick = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Song added to queue",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    onOpenQueue = { isQueueExpanded = true },
                                    isLiveSession = isLiveSession,
                                    onCastClick = { isListenTogetherSheetOpen = true }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isPlayerExpanded,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        activeTrack?.let { track ->
                            PlayerScreen(
                                track = track,
                                isPlaying = isPlaying,
                                progress = progress,
                                isLiked = isLiked,
                                shuffleEnabled = shuffleEnabled,
                                repeatMode = repeatMode,
                                onTogglePlay = { 
                                    isInitiator = true
                                    musicService?.togglePlayPause()
                                    if (isLiveSession) {
                                        sessionViewModel.syncPlayPause(!isPlaying, musicService?.player?.currentPosition ?: 0L)
                                    }
                                },
                                onCollapse = { isPlayerExpanded = false },
                                onSeekToFraction = { f -> 
                                    isInitiator = true
                                    musicService?.seekToFraction(f)
                                    if (isLiveSession) {
                                        val pos = ((musicService?.player?.duration?.coerceAtLeast(0L) ?: 0L) * f).toLong()
                                        sessionViewModel.syncSeek(pos)
                                    }
                                },
                                onSkipNext = { 
                                    isInitiator = true
                                    musicService?.skipToNext() 
                                },
                                onSkipPrevious = { 
                                    isInitiator = true
                                    musicService?.skipToPrevious() 
                                },
                                onSaveTrackClick = onSaveTrackClick,
                                onToggleShuffle = { musicService?.toggleShuffle() },
                                onCycleRepeat = { musicService?.cycleRepeatMode() },
                                isLiveSession = isLiveSession,
                                onCastClick = { isListenTogetherSheetOpen = true }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isQueueExpanded,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        QueueScreen(
                            queue = queue,
                            currentQueueIndex = currentQueueIndex,
                            onMove = { from, to -> musicService?.moveQueueItem(from, to) },
                            onRemove = { index -> musicService?.removeQueueItem(index) },
                            onClose = { isQueueExpanded = false }
                        )
                    }


                    CreateBottomSheet(
                        isVisible = isCreateSheetOpen,
                        onDismiss = { isCreateSheetOpen = false },
                        onCreatePlaylist = { navController.navigate("create_playlist?isCollaborative=false") },
                        onCreateCollaborative = { navController.navigate("create_playlist?isCollaborative=true") }
                    )

                    ProfileDrawer(
                        isOpen = isProfileDrawerOpen,
                        onClose = { isProfileDrawerOpen = false },
                        onViewProfile = { 
                            isProfileDrawerOpen = false
                            navController.navigate("profile")
                        },
                        onNavigate = { route ->
                            isProfileDrawerOpen = false
                            if (route == "logout") {
                                musicService?.stopAndClear()
                                authViewModel.logout()
                            } else {
                                navController.navigate(route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                    
                    if (isListenTogetherSheetOpen) {
                        com.premium.spotifyclone.ui.components.ListenTogetherSheet(
                            onDismiss = { isListenTogetherSheetOpen = false },
                            onStartSession = { code ->
                                sessionViewModel.createOrJoinRoom(code, "User")
                                // Do not close the sheet here; the sheet will now display the code and share options
                            },
                            onJoinSession = { code ->
                                sessionViewModel.createOrJoinRoom(code, "User")
                                isListenTogetherSheetOpen = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Joined Session: $code")
                                }
                            },
                            activeRoomCode = sessionViewModel.currentRoom.collectAsState().value,
                            onEndSession = { sessionViewModel.leaveRoom() }
                        )
                    }

                    // Snackbar overlay on top of everything
                    androidx.compose.material3.SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 120.dp) // padding to stay above mini player or full player controls
                    ) { data ->
                        androidx.compose.material3.Snackbar(
                            snackbarData = data,
                            containerColor = androidx.compose.ui.graphics.Color(0xFF333333),
                            contentColor = androidx.compose.ui.graphics.Color.White,
                            actionColor = androidx.compose.ui.graphics.Color(0xFF1DB954),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }
    }

    private fun bindToService() {
        if (!isBound && serviceStarted) {
            val bindIntent = Intent(this, MusicPlayerService::class.java)
            bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE)
        }
    }

    override fun onStart() {
        super.onStart()
        bindToService()
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: Exception) {
                // Already unbound or registration issue
            }
            isBound = false
        }
    }
}
