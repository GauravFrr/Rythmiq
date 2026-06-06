package com.premium.spotifyclone.data.network

import android.os.Build

/**
 * Picks a dev base URL that can reach a server running on the host machine.
 *
 * - **Android Emulator**: `127.0.0.1` is the emulator itself; use `10.0.2.2` (special alias to host loopback).
 * - **Physical device**: `127.0.0.1` is the phone; use `adb reverse tcp:3000 tcp:3000` so localhost on the
 *   device forwards to your PC, **or** set [OVERRIDE_DEV_API_BASE_URL] to your PC's LAN IP, e.g. `http://192.168.1.42:3000/`.
 */
internal object DevApiBaseUrl {

    /**
     * Optional: set to your PC's LAN URL when testing on a real phone without `adb reverse`.
     * Must include trailing slash to match Retrofit's `baseUrl` rules. Leave null to use emulator / adb-reverse defaults.
     */
    val OVERRIDE_DEV_API_BASE_URL: String? = "https://api.gauravxd.dev/"

    fun resolve(): String {
        OVERRIDE_DEV_API_BASE_URL?.let { return it }
        return if (isProbablyEmulator()) "http://10.0.2.2:3000/" else "http://127.0.0.1:3000/"
    }

    private fun isProbablyEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.lowercase().contains("emu")
            || Build.MODEL.contains("Emulator", ignoreCase = true)
            || Build.MANUFACTURER.contains("Genymotion", ignoreCase = true)
            || (Build.BRAND.startsWith("generic", ignoreCase = true) && Build.DEVICE.startsWith("generic", ignoreCase = true))
            || "google_sdk" == Build.PRODUCT
            || Build.HARDWARE.contains("goldfish", ignoreCase = true)
            || Build.HARDWARE.contains("ranchu", ignoreCase = true)
    }
}
