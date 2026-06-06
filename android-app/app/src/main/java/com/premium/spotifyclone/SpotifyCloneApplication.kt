package com.premium.spotifyclone

import android.app.Application
import com.premium.spotifyclone.data.local.AppDatabase

class SpotifyCloneApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
