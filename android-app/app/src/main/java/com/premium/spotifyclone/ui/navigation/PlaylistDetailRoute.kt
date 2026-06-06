package com.premium.spotifyclone.ui.navigation

object PlaylistDetailRoute {
    const val pattern = "playlist/{playlistId}"
    fun route(playlistId: String) = "playlist/$playlistId"
}
