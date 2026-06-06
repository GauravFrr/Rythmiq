package com.premium.spotifyclone.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.ui.screens.HomeScreen
import com.premium.spotifyclone.ui.screens.HotAndNewScreen
import com.premium.spotifyclone.ui.screens.ChartsScreen
import com.premium.spotifyclone.ui.screens.LibraryScreen
import com.premium.spotifyclone.ui.screens.LikedSongsDetailScreen
import com.premium.spotifyclone.ui.screens.PlaylistDetailScreen
import com.premium.spotifyclone.ui.screens.SearchScreen

import com.premium.spotifyclone.ui.screens.CreatePlaylistScreen
import com.premium.spotifyclone.ui.screens.ProfileScreen
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.premium.spotifyclone.data.local.AppDatabase
import com.premium.spotifyclone.data.local.PlaylistEntity
import androidx.compose.ui.platform.LocalContext

@Composable
fun SpotifyNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onPlayTracks: (List<Track>) -> Unit = {},
    onAddToQueue: (Track) -> Unit = {},
    shuffleEnabled: Boolean = false,
    onToggleShuffle: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    NavHost(
        navController = navController,
        startDestination = "home_tab",
        modifier = modifier,
        enterTransition = {
            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) +
            androidx.compose.animation.slideInHorizontally(
                animationSpec = androidx.compose.animation.core.tween(300),
                initialOffsetX = { it / 8 }
            )
        },
        exitTransition = {
            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) +
            androidx.compose.animation.slideOutHorizontally(
                animationSpec = androidx.compose.animation.core.tween(300),
                targetOffsetX = { -it / 8 }
            )
        },
        popEnterTransition = {
            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) +
            androidx.compose.animation.slideInHorizontally(
                animationSpec = androidx.compose.animation.core.tween(300),
                initialOffsetX = { -it / 8 }
            )
        },
        popExitTransition = {
            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) +
            androidx.compose.animation.slideOutHorizontally(
                animationSpec = androidx.compose.animation.core.tween(300),
                targetOffsetX = { it / 8 }
            )
        }
    ) {
        composable(route = "home_tab") {
            HomeScreen(
                onPlayTracks = onPlayTracks,
                onOpenDrawer = onOpenDrawer
            )
        }
        composable(route = "hot_and_new_tab") {
            HotAndNewScreen(onPlayTracks = onPlayTracks)
        }
        composable(route = "charts_tab") {
            ChartsScreen(onPlayTracks = onPlayTracks)
        }
        composable(route = "library_tab") {
            LibraryScreen(navController = navController, onPlayTracks = onPlayTracks)
        }
        
        // Deep links & secondary screens
        composable(route = Screen.Search.route) {
            SearchScreen(
                onPlayTracks = onPlayTracks,
                onAddToQueue = onAddToQueue,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = LikedSongsDetailRoute.route) {
            LikedSongsDetailScreen(
                onBack = { navController.popBackStack() },
                onPlayTracks = onPlayTracks,
                shuffleEnabled = shuffleEnabled,
                onToggleShuffle = onToggleShuffle,
            )
        }
        composable(
            route = PlaylistDetailRoute.pattern,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.StringType }
            )
        ) { entry ->
            val playlistId = entry.arguments?.getString("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                onBack = { navController.popBackStack() },
                onPlayTracks = onPlayTracks,
                shuffleEnabled = shuffleEnabled,
                onToggleShuffle = onToggleShuffle,
            )
        }
        composable(route = "profile") {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "create_playlist?isCollaborative={isCollaborative}",
            arguments = listOf(
                navArgument("isCollaborative") { 
                    type = NavType.BoolType
                    defaultValue = false 
                }
            )
        ) { entry ->
            val isCollaborative = entry.arguments?.getBoolean("isCollaborative") ?: false
            CreatePlaylistScreen(
                onNavigateBack = { navController.popBackStack() },
                isCollaborative = isCollaborative,
                onSavePlaylist = { id, name ->
                    coroutineScope.launch {
                        AppDatabase.getInstance(context).playlistDao().insert(
                            PlaylistEntity(id = id, name = name)
                        )
                    }
                }
            )
        }
        composable(route = "settings") {
            com.premium.spotifyclone.ui.screens.SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = "recents") {
            com.premium.spotifyclone.ui.screens.RecentlyPlayedScreen(
                onNavigateBack = { navController.popBackStack() },
                onPlayTracks = onPlayTracks
            )
        }
        composable(route = "notifications") {
            com.premium.spotifyclone.ui.screens.NotificationsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
