package com.premium.spotifyclone.ui.models

import com.premium.spotifyclone.data.local.PlaylistEntity

data class PlaylistUiModel(
    val id: String,
    val name: String,
    val coverUrls: List<String?>
)
