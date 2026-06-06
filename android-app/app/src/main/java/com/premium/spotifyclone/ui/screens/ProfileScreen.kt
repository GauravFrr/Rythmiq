package com.premium.spotifyclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    
    var userName by remember { mutableStateOf(com.premium.spotifyclone.util.UserPrefs.getName(context) ?: user?.displayName ?: "User") }
    val photoUrl = com.premium.spotifyclone.util.UserPrefs.getPhotoUrl(context) ?: user?.photoUrl?.toString()
    
    val savedUsername = com.premium.spotifyclone.util.UserPrefs.getUsername(context)
    val displayHandle = if (!savedUsername.isNullOrBlank()) {
        if (savedUsername.startsWith("@")) savedUsername else "@$savedUsername"
    } else {
        "@" + userName.lowercase().replace("\\s".toRegex(), "") + (10..99).random()
    }

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(userName) }
    var editUsername by remember { mutableStateOf(savedUsername ?: "") }
    var editError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var followingCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            val followed = com.premium.spotifyclone.data.network.RetrofitInstance.api.getFollowingArtists()
            followingCount = followed.size
        } catch (e: Exception) {
            // Ignore error, followingCount remains 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        // Profile Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF404040), Color(0xFF121212)),
                        startY = 0f,
                        endY = 600f
                    )
                )
                .padding(top = 20.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF535353)),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        userName.firstOrNull()?.uppercase() ?: "U",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Display Name & Username Editing
            if (isEditing) {
                TextField(
                    value = editName,
                    onValueChange = { editName = it },
                    modifier = Modifier.padding(horizontal = 32.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color(0xFF1DB954)
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    singleLine = true,
                    placeholder = { Text("First Name", color = Color.Gray) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = editUsername,
                    onValueChange = { editUsername = it.filter { char -> !char.isWhitespace() } },
                    modifier = Modifier.padding(horizontal = 32.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color(0xFF1DB954)
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    singleLine = true,
                    placeholder = { Text("Username", color = Color.Gray) }
                )
                if (editError != null) {
                    Text(text = editError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { 
                            isEditing = false 
                            editError = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF535353)),
                        enabled = !isSaving
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    Button(
                        onClick = {
                            if (editName.isNotBlank() && editUsername.isNotBlank()) {
                                isSaving = true
                                editError = null
                                scope.launch {
                                    try {
                                        // Only check username if it changed
                                        val isAvailable = if (editUsername != savedUsername) {
                                            val response = com.premium.spotifyclone.data.network.RetrofitInstance.api.checkUsername(editUsername)
                                            response.body()?.available == true
                                        } else {
                                            true
                                        }

                                        if (!isAvailable) {
                                            editError = "Username is already taken"
                                            isSaving = false
                                            return@launch
                                        }

                                        // Update Firebase Profile
                                        userName = editName.trim()
                                        val updates = UserProfileChangeRequest.Builder()
                                            .setDisplayName(userName)
                                            .build()
                                        user?.updateProfile(updates)
                                        com.premium.spotifyclone.util.UserPrefs.saveName(context, userName)
                                        com.premium.spotifyclone.util.UserPrefs.saveUsername(context, editUsername)
                                        
                                        // Sync with backend
                                        val request = com.premium.spotifyclone.data.api.SyncRequest(
                                            name = userName,
                                            username = editUsername,
                                            photoUrl = photoUrl,
                                            loginMethod = "email" // placeholder
                                        )
                                        com.premium.spotifyclone.data.network.RetrofitInstance.api.syncUser(request)

                                        isEditing = false
                                    } catch (e: Exception) {
                                        editError = "Failed to update profile"
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            } else {
                                editError = "Fields cannot be empty"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            userName,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                editName = userName
                                isEditing = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit name",
                                tint = Color(0xFFB3B3B3),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(displayHandle, color = Color(0xFFB3B3B3), fontSize = 16.sp)
                    user?.email?.let { email ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(email, color = Color(0xFF888888), fontSize = 14.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("0 Followers", color = Color(0xFFB3B3B3), fontSize = 14.sp)
                Text("$followingCount Following", color = Color(0xFFB3B3B3), fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Settings Section
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Find friends",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            HorizontalDivider(color = Color(0xFF2A2A2A))
            Text(
                "Share profile",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}
