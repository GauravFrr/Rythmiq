package com.premium.spotifyclone.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class SessionViewModel : ViewModel() {
    private var mSocket: Socket? = null
    
    // Change this to your local IP for real device testing, e.g. "http://192.168.1.X:3000"
    // Use http://10.0.2.2:3000 for Android emulator
    private val serverUrl = "http://10.0.2.2:3000"

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _currentRoom = MutableStateFlow<String?>(null)
    val currentRoom: StateFlow<String?> = _currentRoom

    init {
        try {
            mSocket = IO.socket(serverUrl)
            setupSocketListeners()
            mSocket?.connect()
        } catch (e: Exception) {
            Log.e("SessionViewModel", "Socket init failed", e)
        }
    }

    private fun setupSocketListeners() {
        mSocket?.on(Socket.EVENT_CONNECT) {
            _isConnected.value = true
            Log.d("SessionViewModel", "Connected to server")
        }
        
        mSocket?.on(Socket.EVENT_DISCONNECT) {
            _isConnected.value = false
            Log.d("SessionViewModel", "Disconnected from server")
        }
        
        mSocket?.on("user_joined") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val userId = data.optString("userId")
                Log.d("SessionViewModel", "User joined: $userId")
            }
        }
    }

    fun createOrJoinRoom(roomCode: String, userId: String) {
        _currentRoom.value = roomCode
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("userId", userId)
        }
        mSocket?.emit("join_room", payload)
    }

    fun syncPlayPause(isPlaying: Boolean, position: Long) {
        val room = _currentRoom.value ?: return
        val payload = JSONObject().apply {
            put("roomCode", room)
            put("isPlaying", isPlaying)
            put("timestamp", position)
        }
        mSocket?.emit("play_pause", payload)
    }

    fun syncSeek(position: Long) {
        val room = _currentRoom.value ?: return
        val payload = JSONObject().apply {
            put("roomCode", room)
            put("position", position)
        }
        mSocket?.emit("seek", payload)
    }

    override fun onCleared() {
        super.onCleared()
        mSocket?.disconnect()
        mSocket?.off()
    }
}
