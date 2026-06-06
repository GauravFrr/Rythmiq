package com.premium.spotifyclone.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MockAuthManager {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    fun login(username: String) {
        _username.value = username
        _isLoggedIn.value = true
    }

    fun logout() {
        _username.value = null
        _isLoggedIn.value = false
    }
}
