package com.premium.spotifyclone.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.premium.spotifyclone.ui.theme.AccentRed
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily

val TextPrimary = Color.White
val TextSecondary = Color(0xFFAAAAAA)

@Composable
fun AuthBanner() {
    val images = listOf(
        "https://images.unsplash.com/photo-1493225457124-a1a2a5f5f9af?q=80&w=500&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?q=80&w=500&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=500&auto=format&fit=crop"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
    ) {
        Row(modifier = Modifier.fillMaxSize().blur(8.dp)) {
            images.forEach { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
        )
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.premium.spotifyclone.R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-16).dp)
                .clip(CircleShape)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    title: String,
    subtitle: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    buttonText: String,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val focusManager = LocalFocusManager.current
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
    }) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box {
                AuthBanner()
                AuthTopBar(onBack = onBack)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(title, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(subtitle, color = TextSecondary, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(

                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(placeholder, color = Color(0xFF666666)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedIndicatorColor = AccentRed,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = errorMessage, 
                            color = AccentRed, 
                            fontSize = 14.sp, 
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        disabledContainerColor = AccentRed.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    enabled = value.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(buttonText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneInputScreen(
    title: String,
    subtitle: String,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountryCode: String,
    onCountryCodeChange: (String) -> Unit,
    buttonText: String,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val focusManager = LocalFocusManager.current
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val countries = remember {
        listOf(
            "+91" to "India", "+1" to "USA/Canada", "+44" to "UK", "+61" to "Australia",
            "+81" to "Japan", "+49" to "Germany", "+33" to "France", "+55" to "Brazil",
            "+86" to "China", "+7" to "Russia", "+39" to "Italy", "+34" to "Spain",
            "+52" to "Mexico", "+62" to "Indonesia", "+92" to "Pakistan", "+880" to "Bangladesh",
            "+234" to "Nigeria", "+27" to "South Africa", "+20" to "Egypt", "+90" to "Turkey"
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
    }) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box {
                AuthBanner()
                AuthTopBar(onBack = onBack)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(title, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(subtitle, color = TextSecondary, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Country Code Selector
                    Box(modifier = Modifier.weight(0.3f)) {
                        OutlinedTextField(
                            value = selectedCountryCode,
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { isDropdownExpanded = true },
                            enabled = false,
                            colors = TextFieldDefaults.colors(
                                disabledContainerColor = Color(0xFF1A1A1A),
                                disabledIndicatorColor = Color.Transparent,
                                disabledTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, color = Color.White, fontFamily = FontFamily.SansSerif)
                        )
                        
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF222222)).fillMaxWidth(0.8f).heightIn(max = 300.dp)
                        ) {
                            OutlinedTextField(

                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search...", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF1A1A1A),
                                    unfocusedContainerColor = Color(0xFF1A1A1A),
                                    focusedIndicatorColor = AccentRed,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                            val filtered = countries.filter { 
                                it.first.contains(searchQuery, ignoreCase = true) || 
                                it.second.contains(searchQuery, ignoreCase = true) 
                            }
                            filtered.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text("$name ($code)", color = Color.White) },
                                    onClick = {
                                        onCountryCodeChange(code)
                                        isDropdownExpanded = false
                                        searchQuery = ""
                                    }
                                )
                            }
                        }
                        
                        // Invisible box to handle clicks over disabled TextField
                        Box(modifier = Modifier.matchParentSize().clickable { isDropdownExpanded = true })
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Phone Number Input
                    OutlinedTextField(

                        value = phoneNumber,
                        onValueChange = onPhoneNumberChange,
                        placeholder = { Text("Phone Number", color = Color(0xFF666666)) },
                        modifier = Modifier.weight(0.7f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1A1A1A),
                            unfocusedContainerColor = Color(0xFF1A1A1A),
                            focusedIndicatorColor = AccentRed,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp, fontFamily = FontFamily.SansSerif)
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = errorMessage, 
                            color = AccentRed, 
                            fontSize = 14.sp, 
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        disabledContainerColor = AccentRed.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    enabled = phoneNumber.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(buttonText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun OtpScreen(
    phoneOrEmail: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var timeLeft by remember { mutableStateOf(60) }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
    }) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box {
                AuthBanner()
                AuthTopBar(onBack = onBack)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text("Verify it's you", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("We sent a 6-digit code to $phoneOrEmail", color = TextSecondary, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(32.dp))

                // Basic OTP Input Box
                OutlinedTextField(

                    value = otp,
                    onValueChange = { if (it.length <= 6) onOtpChange(it) },
                    placeholder = { Text("6-digit code", color = Color(0xFF666666)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedIndicatorColor = AccentRed,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = errorMessage, 
                            color = AccentRed, 
                            fontSize = 14.sp, 
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    if (timeLeft > 0) {
                        Text("Resend code in ${timeLeft}s", color = TextSecondary, fontSize = 14.sp)
                    } else {
                        Text(
                            "Resend code", 
                            color = AccentRed, 
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                timeLeft = 60
                                onResend()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onVerify,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        disabledContainerColor = AccentRed.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    enabled = otp.length == 6 && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Verify & Continue", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// Custom animation for sliding between Name and Username
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProfileSetupScreen(
    isGoogle: Boolean = false,
    onComplete: (String, String) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var step by remember { mutableStateOf(if (isGoogle) 1 else 0) } // 0 = Name, 1 = Username
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    Box(modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
    }) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box {
                AuthBanner()
                AuthTopBar(onBack = {
                    if (step == 1 && !isGoogle) step = 0 else onBack()
                })
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(animationSpec = tween(500)) { width -> width } + fadeIn(animationSpec = tween(500)) with
                        slideOutHorizontally(animationSpec = tween(500)) { width -> -width } + fadeOut(animationSpec = tween(500))
                    } else {
                        slideInHorizontally(animationSpec = tween(500)) { width -> -width } + fadeIn(animationSpec = tween(500)) with
                        slideOutHorizontally(animationSpec = tween(500)) { width -> width } + fadeOut(animationSpec = tween(500))
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { targetStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    if (targetStep == 0) {
                        Text("What's your name?", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("This appears on your Rythmiq profile.", color = TextSecondary, fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        OutlinedTextField(

                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Display Name", color = Color(0xFF666666)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1A1A1A),
                                unfocusedContainerColor = Color(0xFF1A1A1A),
                                focusedIndicatorColor = AccentRed,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Text("Pick a username", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Make it unique. You can change this later.", color = TextSecondary, fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        OutlinedTextField(

                            value = username,
                            onValueChange = { username = it.filter { char -> !char.isWhitespace() } },
                            placeholder = { Text("Username", color = Color(0xFF666666)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1A1A1A),
                                unfocusedContainerColor = Color(0xFF1A1A1A),
                                focusedIndicatorColor = AccentRed,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = errorMessage, 
                                color = AccentRed, 
                                fontSize = 14.sp, 
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            if (targetStep == 0) step = 1
                            else onComplete(name, username)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentRed,
                            disabledContainerColor = AccentRed.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        enabled = (targetStep == 0 && name.isNotBlank()) || (targetStep == 1 && username.isNotBlank()) && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(if (targetStep == 0) "Next" else "Create Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailAuthScreen(
    initialIsSignup: Boolean = false,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit,
    onSignup: (String, String, String) -> Unit,
    onForgotPassword: (String) -> Unit
) {
    var isSignup by remember { mutableStateOf(initialIsSignup) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
    }) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box {
                AuthBanner()
                AuthTopBar(onBack = onBack)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = if (isSignup) "Create account" else "Log in",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isSignup) "Sign up to start listening." else "Welcome back! Log in to your account.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isSignup) {
                    OutlinedTextField(

                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Full Name", color = Color(0xFF666666)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentRed,
                            unfocusedBorderColor = Color(0xFF333333),
                            cursorColor = AccentRed
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(

                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email address", color = Color(0xFF666666)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentRed,
                        unfocusedBorderColor = Color(0xFF333333),
                        cursorColor = AccentRed
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(

                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password", color = Color(0xFF666666)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentRed,
                        unfocusedBorderColor = Color(0xFF333333),
                        cursorColor = AccentRed
                    ),
                    singleLine = true
                )

                if (isSignup) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(

                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = { Text("Confirm Password", color = Color(0xFF666666)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentRed,
                            unfocusedBorderColor = Color(0xFF333333),
                            cursorColor = AccentRed
                        ),
                        singleLine = true
                    )
                } else {
                    Text(
                        text = "Forgot Password?",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable {
                                onForgotPassword(email)
                            },
                        textAlign = TextAlign.End
                    )
                }

                val displayError = validationError ?: errorMessage
                if (displayError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x33FF0000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x66FF0000), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = displayError,
                            color = AccentRed,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        validationError = null
                        if (isSignup) {
                            if (name.isBlank()) {
                                validationError = "Name cannot be empty."
                            } else if (password.length < 8) {
                                validationError = "Password must be at least 8 characters."
                            } else if (password != confirmPassword) {
                                validationError = "Passwords do not match."
                            } else {
                                onSignup(name, email, password)
                            }
                        } else {
                            if (email.isBlank() || password.isBlank()) {
                                validationError = "Please enter email and password."
                            } else {
                                onLogin(email, password)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (isSignup) "Create Account" else "Log In",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isSignup) "Already have an account? " else "Don't have an account? ",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isSignup) "Log in" else "Sign up",
                        color = AccentRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { 
                            isSignup = !isSignup 
                            validationError = null
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
