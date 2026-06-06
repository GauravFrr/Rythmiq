package com.premium.spotifyclone.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.premium.spotifyclone.ui.theme.AccentRed

@Composable
fun ProfileDrawer(
    isOpen: Boolean,
    userName: String = "User",
    onClose: () -> Unit,
    onViewProfile: () -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it }),
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val user = FirebaseAuth.getInstance().currentUser
        val displayName = com.premium.spotifyclone.util.UserPrefs.getName(context) ?: user?.displayName ?: userName
        val photoUrl = com.premium.spotifyclone.util.UserPrefs.getPhotoUrl(context) ?: user?.photoUrl?.toString()
        
        var expandedPronouns by remember { mutableStateOf(false) }
        var selectedPronoun by remember { mutableStateOf(com.premium.spotifyclone.util.UserPrefs.getPronouns(context) ?: "Add pronouns") }
        Row(modifier = Modifier.fillMaxSize()) {
            // Drawer panel
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.82f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A1A1A), Color(0xFF121212))
                        )
                    )
                    .statusBarsPadding()
                    .padding(top = 20.dp)
            ) {
                // Profile header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewProfile() }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(AccentRed),
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
                                displayName.firstOrNull()?.uppercase() ?: "U",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            displayName.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "View profile",
                            color = Color(0xFFB3B3B3),
                            fontSize = 13.sp
                        )
                    }
                }

                // Pronouns selector
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF282828))
                            .clickable { expandedPronouns = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PersonOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(selectedPronoun, color = Color.White, fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = expandedPronouns,
                        onDismissRequest = { expandedPronouns = false },
                        modifier = Modifier.background(Color(0xFF282828))
                    ) {
                        DropdownMenuItem(
                            text = { Text("He/Him", color = Color.White) },
                            onClick = { 
                                selectedPronoun = "He/Him"
                                com.premium.spotifyclone.util.UserPrefs.savePronouns(context, "He/Him")
                                expandedPronouns = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("She/Her", color = Color.White) },
                            onClick = { 
                                selectedPronoun = "She/Her"
                                com.premium.spotifyclone.util.UserPrefs.savePronouns(context, "She/Her")
                                expandedPronouns = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("They/Them", color = Color.White) },
                            onClick = { 
                                selectedPronoun = "They/Them"
                                com.premium.spotifyclone.util.UserPrefs.savePronouns(context, "They/Them")
                                expandedPronouns = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Prefer not to say", color = Color.White) },
                            onClick = { 
                                selectedPronoun = "Add pronouns"
                                com.premium.spotifyclone.util.UserPrefs.savePronouns(context, "Add pronouns")
                                expandedPronouns = false 
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Premium Menu items
                DrawerItem(icon = Icons.Default.History, label = "Recently Played") {
                    onNavigate("recents")
                }
                DrawerItem(icon = Icons.Default.Notifications, label = "Notifications") {
                    onNavigate("notifications")
                }
                DrawerItem(icon = Icons.Rounded.Settings, label = "Settings") {
                    onNavigate("settings")
                }

                Spacer(modifier = Modifier.weight(1f))
                
                HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 1.dp)
                DrawerItem(icon = Icons.AutoMirrored.Filled.ExitToApp, label = "Log Out") {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    com.premium.spotifyclone.data.api.BackendApiClient.authToken = null
                    onNavigate("logout")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Scrim (tap to close)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onClose() }
            )
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    showBadge: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color(0xFFE0E0E0),
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            label,
            color = Color(0xFFE0E0E0),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (showBadge) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4A90D9))
            )
        }
    }
}
