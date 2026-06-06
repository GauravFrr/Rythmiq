package com.premium.spotifyclone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasteProfileSetupScreen(
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1 for Genres, 2 for Artists
    val selectedGenres = remember { mutableStateListOf<String>() }
    val selectedArtists = remember { mutableStateListOf<String>() }
    val coroutineScope = rememberCoroutineScope()

    val genres = listOf("Romantic", "Party", "Sad", "Lo-Fi", "Hip Hop", "Devotional", "Pop", "Classical")
    val artists = listOf("Arijit Singh", "AP Dhillon", "Shreya Ghoshal", "Badshah", "Neha Kakkar", "Atif Aslam", "Kishore Kumar", "Lata Mangeshkar")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = if (step == 1) "What's your vibe?" else "Who do you love?",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (step == 1) "Pick 3 or more genres you love." else "Pick 3 or more artists you listen to.",
                color = Color.LightGray,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            val currentList = if (step == 1) genres else artists
            val currentSelected = if (step == 1) selectedGenres else selectedArtists

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(currentList) { item ->
                    val isSelected = currentSelected.contains(item)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) currentSelected.remove(item)
                            else currentSelected.add(item)
                        },
                        label = { Text(item) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1DB954),
                            selectedLabelColor = Color.White,
                            containerColor = Color.DarkGray,
                            labelColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                }
            }
            
            Button(
                onClick = {
                    if (step == 1) {
                        step = 2
                    } else {
                        // TODO: Make API call to seed user_taste_profile
                        // For now we just proceed
                        onComplete()
                    }
                },
                enabled = currentSelected.size >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(if (step == 1) "Next" else "Finish", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
