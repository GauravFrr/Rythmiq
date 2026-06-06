package com.premium.spotifyclone.ui.utils

import com.premium.spotifyclone.data.models.Track

/** Up to 5 tracks from the tapped item (same list order) for queue playback. */
fun playWindowFromList(all: List<Track>, track: Track): List<Track> {
    val withAudio = all.filter { it.audioUrl != null }
    val idx = withAudio.indexOfFirst { it.id == track.id }
    return if (idx >= 0) withAudio.drop(idx).take(5) else listOfNotNull(track.takeIf { it.audioUrl != null })
}
