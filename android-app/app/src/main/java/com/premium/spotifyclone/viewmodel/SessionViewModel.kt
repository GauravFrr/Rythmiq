package com.premium.spotifyclone.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.premium.spotifyclone.data.models.Track
import com.premium.spotifyclone.data.network.DevApiBaseUrl
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class SessionViewModel : ViewModel() {
    private var mSocket: Socket? = null
    
    private val serverUrl = DevApiBaseUrl.resolve()
    private val gson = Gson()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _currentRoom = MutableStateFlow<String?>(null)
    val currentRoom: StateFlow<String?> = _currentRoom

    // ── Incoming Sync Events ──────────────────────────────────────────────
    private val _syncPlayPause = MutableSharedFlow<Pair<Boolean, Long>>() // isPlaying, timestamp
    val syncPlayPause: SharedFlow<Pair<Boolean, Long>> = _syncPlayPause

    private val _syncSong = MutableSharedFlow<Track>()
    val syncSong: SharedFlow<Track> = _syncSong

    private val _syncSeek = MutableSharedFlow<Long>()
    val syncSeek: SharedFlow<Long> = _syncSeek

    private val _syncQueue = MutableSharedFlow<Track>()
    val syncQueue: SharedFlow<Track> = _syncQueue

    private val _userJoined = MutableSharedFlow<String>()
    val userJoined: SharedFlow<String> = _userJoined

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
            
            val room = _currentRoom.value
            if (room != null) {
                val payload = JSONObject().apply {
                    put("roomCode", room)
                    put("userId", "User")
                }
                mSocket?.emit("join_room", payload)
            }
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
                viewModelScope.launch {
                    _userJoined.emit(userId)
                }
            }
        }

        mSocket?.on("sync_play_pause") { args ->
            if (_currentRoom.value == null) return@on
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val isPlaying = data.optBoolean("isPlaying")
                val timestamp = data.optLong("timestamp")
                viewModelScope.launch {
                    _syncPlayPause.emit(Pair(isPlaying, timestamp))
                }
            }
        }

        mSocket?.on("sync_song") { args ->
            if (_currentRoom.value == null) return@on
            if (args.isNotEmpty()) {
                try {
                    val data = args[0] as JSONObject
                    val songJson = data.getJSONObject("song").toString()
                    val track = gson.fromJson(songJson, Track::class.java)
                    viewModelScope.launch {
                        _syncSong.emit(track)
                    }
                } catch (e: Exception) {
                    Log.e("SessionViewModel", "Failed to parse sync_song", e)
                }
            }
        }

        mSocket?.on("sync_seek") { args ->
            if (_currentRoom.value == null) return@on
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val position = data.optLong("position")
                viewModelScope.launch {
                    _syncSeek.emit(position)
                }
            }
        }

        mSocket?.on("sync_queue") { args ->
            if (_currentRoom.value == null) return@on
            if (args.isNotEmpty()) {
                try {
                    val data = args[0] as JSONObject
                    val songJson = data.getJSONObject("song").toString()
                    val track = gson.fromJson(songJson, Track::class.java)
                    viewModelScope.launch {
                        _syncQueue.emit(track)
                    }
                } catch (e: Exception) {
                    Log.e("SessionViewModel", "Failed to parse sync_queue", e)
                }
            }
        }
    }

    // ── Outgoing Actions ──────────────────────────────────────────────────

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

    fun syncSong(track: Track) {
        val room = _currentRoom.value ?: return
        try {
            val trackJson = JSONObject(gson.toJson(track))
            val payload = JSONObject().apply {
                put("roomCode", room)
                put("song", trackJson)
            }
            mSocket?.emit("change_song", payload)
        } catch (e: Exception) {
            Log.e("SessionViewModel", "Failed to serialize track for sync", e)
        }
    }

    fun syncQueue(track: Track) {
        val room = _currentRoom.value ?: return
        try {
            val trackJson = JSONObject(gson.toJson(track))
            val payload = JSONObject().apply {
                put("roomCode", room)
                put("song", trackJson)
            }
            mSocket?.emit("add_to_queue", payload)
        } catch (e: Exception) {
            Log.e("SessionViewModel", "Failed to serialize track for queue sync", e)
        }
    }

    fun leaveRoom() {
        val room = _currentRoom.value ?: return
        try {
            val payload = JSONObject().apply {
                put("roomCode", room)
            }
            mSocket?.emit("leave_room", payload) // Backend doesn't explicitly need this if it relies on disconnect, but good practice
        } catch (e: Exception) {
            Log.e("SessionViewModel", "Error leaving room", e)
        }
        _currentRoom.value = null
    }

    override fun onCleared() {
        super.onCleared()
        mSocket?.disconnect()
        mSocket?.off()
    }
}
