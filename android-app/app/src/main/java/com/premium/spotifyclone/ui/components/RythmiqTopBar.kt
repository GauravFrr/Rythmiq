package com.premium.spotifyclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Segment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.premium.spotifyclone.auth.MockAuthManager
import com.premium.spotifyclone.ui.theme.*

@Composable
fun RythmiqTopBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onSearchClick: () -> Unit
) {
    val isLoggedIn by MockAuthManager.isLoggedIn.collectAsState()
    val username by MockAuthManager.username.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBlack)
            .statusBarsPadding()
    ) {
        // Top row: Menu | Search Pill | Avatar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modern Hamburger Menu
            IconButton(onClick = onOpenDrawer, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Segment, contentDescription = "Menu", tint = TextPrimary, modifier = Modifier.size(28.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Search Pill
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppElevated)
                    .clickable { onSearchClick() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search tracks, artists...", color = TextSecondary, fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isLoggedIn) AccentRed else AppElevated)
                    .clickable { onNavigate("profile") },
                contentAlignment = Alignment.Center
            ) {
                if (isLoggedIn) {
                    Text(
                        username?.take(1)?.uppercase() ?: "U",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = "Profile",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        // Tab row
        val tabs = listOf(
            "home_tab" to "Home",
            "hot_and_new_tab" to "Trending",
            "charts_tab" to "New Releases",
            "library_tab" to "Library"
        )
        
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOfFirst { it.first == currentRoute }.coerceAtLeast(0),
            containerColor = AppBlack,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                val idx = tabs.indexOfFirst { it.first == currentRoute }.coerceAtLeast(0)
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                    color = AccentRed,
                    height = 3.dp
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, (route, label) ->
                val selected = currentRoute == route
                Tab(
                    selected = selected,
                    onClick = { onNavigate(route) },
                    text = {
                        Text(
                            text = label,
                            color = if (selected) TextPrimary else TextTertiary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                )
            }
        }
    }
}

