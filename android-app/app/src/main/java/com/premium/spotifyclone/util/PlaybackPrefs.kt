package com.premium.spotifyclone.util

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** Persists shuffle / repeat across process death and new queues. */
object PlaybackPrefs {
    private const val PREFS_NAME = "playback_prefs"
    private const val KEY_SHUFFLE = "shuffle_enabled"
    private const val KEY_REPEAT = "repeat_mode"

    fun loadShuffleEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SHUFFLE, false)

    fun loadRepeatMode(context: Context): Int {
        val r = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_REPEAT, Player.REPEAT_MODE_OFF)
        return when (r) {
            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ONE, Player.REPEAT_MODE_ALL -> r
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun saveShuffleEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SHUFFLE, enabled)
            .apply()
    }

    fun saveRepeatMode(context: Context, @Player.RepeatMode mode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_REPEAT, mode)
            .apply()
    }

    fun applyToPlayer(context: Context, player: ExoPlayer) {
        player.shuffleModeEnabled = loadShuffleEnabled(context)
        player.repeatMode = loadRepeatMode(context)
    }
}
