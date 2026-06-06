package com.premium.spotifyclone.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.premium.spotifyclone.data.api.BackendApiClient
import com.premium.spotifyclone.data.api.SyncRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.premium.spotifyclone.util.UserPrefs
import java.util.concurrent.TimeUnit

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
    object EmailNotVerified : AuthState()
    data class OtpSent(val verificationId: String) : AuthState()
    object NeedsProfileSetup : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val api = BackendApiClient.instance

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow<String?>(UserPrefs.getUsername(application) ?: auth.currentUser?.displayName)
    val username: StateFlow<String?> = _username.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _isLoggedIn.value = firebaseAuth.currentUser != null
            if (firebaseAuth.currentUser != null) {
                _username.value = UserPrefs.getUsername(application) ?: firebaseAuth.currentUser?.displayName
            }
        }
    }

    fun checkSession() {
        _isLoggedIn.value = auth.currentUser != null
        _username.value = UserPrefs.getUsername(getApplication()) ?: auth.currentUser?.displayName
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    // ── Email / Password ──────────────────────────────────────────────
    fun loginWithEmail(email: String, pass: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                if (result.user?.isEmailVerified == true) {
                    syncWithBackend("email")
                } else {
                    _authState.value = AuthState.EmailNotVerified
                    auth.signOut()
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun registerWithEmail(email: String, pass: String, name: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                result.user?.sendEmailVerification()?.await()
                
                // Set name in Firebase Profile
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                result.user?.updateProfile(profileUpdates)?.await()

                _authState.value = AuthState.EmailNotVerified
                auth.signOut() // Force them to login after verifying
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.Error("Check your inbox for a password reset link.") // Hijacking error to show toast message
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }

    // ── Phone Auth ────────────────────────────────────────────────────
    fun sendPhoneOtp(activity: Activity, phoneNumber: String) {
        _authState.value = AuthState.Loading
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-retrieval
                    signInWithPhoneCredential(credential)
                }
                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    _authState.value = AuthState.Error(e.message ?: "Verification failed")
                }
                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    _authState.value = AuthState.OtpSent(verificationId)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneOtp(verificationId: String, code: String) {
        _authState.value = AuthState.Loading
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneCredential(credential)
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                if (result.additionalUserInfo?.isNewUser == true) {
                    _authState.value = AuthState.NeedsProfileSetup
                } else {
                    syncWithBackend("phone")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Invalid OTP")
            }
        }
    }

    fun completeProfileSetup(provider: String, name: String, username: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                auth.currentUser?.updateProfile(profileUpdates)?.await()
                this@AuthViewModel.pendingName = name
                syncWithBackend(provider, username)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Profile setup failed")
            }
        }
    }

    var pendingGoogleToken: String? = null
    var pendingUsername: String? = null
    var pendingName: String? = null
    var pendingPhotoUrl: String? = null

    // ── Google Auth ───────────────────────────────────────────────────
    fun loginWithGoogleToken(idToken: String, displayName: String? = null, photoUrl: String? = null) {
        // Only take the first name
        val firstName = displayName?.split(" ")?.firstOrNull()
        pendingName = firstName
        pendingPhotoUrl = photoUrl
        if (pendingUsername.isNullOrEmpty() && !firstName.isNullOrEmpty()) {
            pendingUsername = firstName.lowercase().replace("\\s".toRegex(), "") + (10..99).random()
        }
        _authState.value = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        viewModelScope.launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                if (result.additionalUserInfo?.isNewUser == true) {
                    _authState.value = AuthState.NeedsProfileSetup
                } else {
                    syncWithBackend("google")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google Sign-in failed")
            }
        }
    }

    // ── Check Username Availability ───────────────────────────────────
    suspend fun checkUsernameAvailability(username: String): Boolean {
        return try {
            val response = api.checkUsername(username)
            if (response.isSuccessful) {
                response.body()?.available == true
            } else {
                false // Treat as taken on error
            }
        } catch (e: Exception) {
            Log.e("Auth", "Check Username Error", e)
            false
        }
    }

    // ── Sync to Supabase via Node Backend ─────────────────────────────
    private suspend fun syncWithBackend(method: String, username: String? = null) {
        try {
            val user = auth.currentUser
            val token = user?.getIdToken(true)?.await()?.token
            
            if (token != null) {
                BackendApiClient.authToken = token // Set token globally for retrofit
                
                val request = SyncRequest(
                    name = pendingName ?: user.displayName ?: "User",
                    username = pendingUsername ?: username,
                    photoUrl = pendingPhotoUrl ?: user.photoUrl?.toString(),
                    loginMethod = method
                )
                val response = api.syncUser(request)
                if (response.isSuccessful) {
                    val finalName = pendingName ?: user.displayName ?: "User"
                    val finalUsername = pendingUsername ?: username
                    val finalPhotoUrl = pendingPhotoUrl ?: user.photoUrl?.toString()
                    
                    UserPrefs.saveUser(
                        getApplication(),
                        name = finalName,
                        username = finalUsername,
                        pronouns = null,
                        photoUrl = finalPhotoUrl
                    )
                    
                    _isLoggedIn.value = true
                    _username.value = finalUsername ?: user.displayName
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Backend sync failed: ${response.errorBody()?.string()}")
                }
            } else {
                _authState.value = AuthState.Error("Failed to get auth token")
            }
        } catch (e: Exception) {
            Log.e("Auth", "Sync Error", e)
            _authState.value = AuthState.Error(e.message ?: "Sync error")
        }
    }

    fun logout() {
        auth.signOut()
        BackendApiClient.authToken = null
        UserPrefs.clear(getApplication())
        _isLoggedIn.value = false
        _username.value = null
        _authState.value = AuthState.Idle
    }
}
