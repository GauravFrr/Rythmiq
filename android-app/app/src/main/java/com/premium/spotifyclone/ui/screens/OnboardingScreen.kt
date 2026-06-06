package com.premium.spotifyclone.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.premium.spotifyclone.R

@Composable
fun OnboardingScreen(
    onContinueWithEmail: () -> Unit,
    onContinueWithGoogle: () -> Unit,
    onContinueWithPhone: () -> Unit,
    onSignUp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // TOP SECTION: Images Grid + Logo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Takes all space above buttons
            contentAlignment = Alignment.Center
        ) {
            // 2x3 Grid of Blurred Images
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(8.dp)
            ) {
                val images = listOf(
                    "https://images.unsplash.com/photo-1493225457124-a1a2a5f5f9af?q=80&w=500&auto=format&fit=crop", // Singer
                    "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?q=80&w=500&auto=format&fit=crop", // Concert
                    "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=500&auto=format&fit=crop", // Party
                    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=500&auto=format&fit=crop", // Mic
                    "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=500&auto=format&fit=crop", // DJ
                    "https://images.unsplash.com/photo-1507838153414-b4b713384a76?q=80&w=500&auto=format&fit=crop"  // Record Player
                )
                for (i in 0 until 2) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = images[i * 3],
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = images[i * 3 + 1],
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = images[i * 3 + 2],
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Dark gradient overlay to blend into the black buttons area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.8f),
                                Color.Black.copy(alpha = 1f) // Blends flawlessly into the bottom black background
                            )
                        )
                    )
            )

            // Logo & Title Centered over the images
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Rythmiq Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Rythmiq",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Millions of songs.",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Free on Rythmiq.",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        }

        // BOTTOM SECTION: Buttons Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onContinueWithGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Continue with Google",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onContinueWithPhone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Continue with Phone",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onContinueWithEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Continue with Email",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "New user? Sign up",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.clickable { onSignUp() }.padding(8.dp)
            )
        }
    }
}
