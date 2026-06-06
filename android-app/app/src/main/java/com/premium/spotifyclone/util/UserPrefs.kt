package com.premium.spotifyclone.util

import android.content.Context
import android.content.SharedPreferences

object UserPrefs {
    private const val PREFS_NAME = "rythmiq_user_prefs"
    
    private const val KEY_NAME = "user_name"
    private const val KEY_USERNAME = "user_username"
    private const val KEY_PRONOUNS = "user_pronouns"
    private const val KEY_PHOTO_URL = "user_photo_url"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveUser(context: Context, name: String?, username: String?, pronouns: String?, photoUrl: String?) {
        getPrefs(context).edit().apply {
            name?.let { putString(KEY_NAME, it) }
            username?.let { putString(KEY_USERNAME, it) }
            pronouns?.let { putString(KEY_PRONOUNS, it) }
            photoUrl?.let { putString(KEY_PHOTO_URL, it) }
            apply()
        }
    }

    fun savePronouns(context: Context, pronouns: String) {
        getPrefs(context).edit().putString(KEY_PRONOUNS, pronouns).apply()
    }
    
    fun saveName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_NAME, name).apply()
    }

    fun saveUsername(context: Context, username: String) {
        getPrefs(context).edit().putString(KEY_USERNAME, username).apply()
    }

    fun getName(context: Context): String? = getPrefs(context).getString(KEY_NAME, null)
    fun getUsername(context: Context): String? = getPrefs(context).getString(KEY_USERNAME, null)
    fun getPronouns(context: Context): String? = getPrefs(context).getString(KEY_PRONOUNS, null)
    fun getPhotoUrl(context: Context): String? = getPrefs(context).getString(KEY_PHOTO_URL, null)

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
