package com.premium.spotifyclone.util

import android.content.Context
import com.premium.spotifyclone.data.models.Track

object LastPlayedPrefs {
    private const val PREFS_NAME = "last_played_track"
    private const val KEY_ID = "track_id"
    private const val KEY_TITLE = "track_title"
    private const val KEY_ARTIST = "track_artist"
    private const val KEY_COVER = "track_cover"
    private const val KEY_AUDIO = "track_audio"
    private const val KEY_DURATION = "track_duration"

    fun save(context: Context, track: Track) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_ID, track.id)
            putString(KEY_TITLE, track.title)
            putString(KEY_ARTIST, track.artist)
            putString(KEY_COVER, track.coverUrl)
            putString(KEY_AUDIO, track.audioUrl)
            putLong(KEY_DURATION, track.durationMs)
            apply()
        }
    }

    fun load(context: Context): Track? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_ID, null) ?: return null
        val title = prefs.getString(KEY_TITLE, null) ?: return null
        val artist = prefs.getString(KEY_ARTIST, null) ?: return null
        val cover = prefs.getString(KEY_COVER, "") ?: ""
        val audio = prefs.getString(KEY_AUDIO, null)
        val duration = prefs.getLong(KEY_DURATION, 0L)
        return Track(id, title, artist, cover, duration, audio)
    }
}
