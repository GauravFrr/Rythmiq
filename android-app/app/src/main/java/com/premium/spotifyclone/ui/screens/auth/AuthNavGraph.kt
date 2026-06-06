package com.premium.spotifyclone.ui.screens.auth

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.premium.spotifyclone.viewmodel.AuthState
import com.premium.spotifyclone.viewmodel.AuthViewModel

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AuthNavGraph(
    initialRoute: String, // "email" or "phone" or "google"
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onBackToOnboarding: () -> Unit
) {
    val navController = rememberNavController()
    val activity = LocalContext.current as Activity
    
    // Shared state across the flow
    var userEmail by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }
    var userPassword by remember { mutableStateOf("") }
    var currentVerificationId by remember { mutableStateOf("") }
    var nameToRegister by remember { mutableStateOf("") }
    var fullPhoneSentTo by remember { mutableStateOf("") }
    
    val authState by authViewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading
    val errorMessage = (authState as? AuthState.Error)?.message

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> onAuthSuccess()
            is AuthState.OtpSent -> {
                currentVerificationId = (authState as AuthState.OtpSent).verificationId
                navController.navigate("phone_otp") { launchSingleTop = true }
            }
            is AuthState.EmailNotVerified -> {
                navController.navigate("email_verification") { launchSingleTop = true }
            }
            is AuthState.NeedsProfileSetup -> {
                navController.navigate("profile_setup") { launchSingleTop = true }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (initialRoute.startsWith("email")) "email_auth" else if (initialRoute == "phone") "phone_input" else "google_loading",
        enterTransition = {
            slideInHorizontally(animationSpec = tween(500)) { it } + fadeIn(animationSpec = tween(500))
        },
        exitTransition = {
            slideOutHorizontally(animationSpec = tween(500)) { -it } + fadeOut(animationSpec = tween(500))
        },
        popEnterTransition = {
            slideInHorizontally(animationSpec = tween(500)) { -it } + fadeIn(animationSpec = tween(500))
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(500)) { it } + fadeOut(animationSpec = tween(500))
        }
    ) {
        
        // ── PHONE FLOW ──────────────────────────────────────────────────────────
        composable("phone_input") {
            var selectedCountryCode by remember { mutableStateOf("+91") }

            PhoneInputScreen(
                title = "Enter phone number",
                subtitle = "We'll send you a code to verify it's you.",
                phoneNumber = userPhone,
                onPhoneNumberChange = { userPhone = it },
                selectedCountryCode = selectedCountryCode,
                onCountryCodeChange = { selectedCountryCode = it },
                buttonText = "Send OTP",
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = onBackToOnboarding,
                onContinue = {
                    authViewModel.clearError()
                    val fullPhone = if (userPhone.startsWith("+")) userPhone else "$selectedCountryCode$userPhone"
                    fullPhoneSentTo = fullPhone
                    authViewModel.sendPhoneOtp(activity, fullPhone)
                }
            )
        }
        
        composable("profile_setup") {
            val scope = rememberCoroutineScope()
            var localLoading by remember { mutableStateOf(false) }
            var localError by remember { mutableStateOf<String?>(null) }
            
            ProfileSetupScreen(
                isGoogle = initialRoute == "google",
                isLoading = isLoading || localLoading,
                errorMessage = localError ?: errorMessage,
                onBack = { navController.popBackStack() },
                onComplete = { name, username ->
                    nameToRegister = name
                    authViewModel.clearError()
                    localError = null
                    localLoading = true
                    scope.launch {
                        val isAvailable = authViewModel.checkUsernameAvailability(username)
                        localLoading = false
                        if (isAvailable) {
                            authViewModel.completeProfileSetup(if (initialRoute == "google") "google" else "phone", name, username)
                        } else {
                            localError = "This username is already taken. Please choose another."
                        }
                    }
                }
            )
        }
        
        composable("phone_otp") {
            var otp by remember { mutableStateOf("") }
            OtpScreen(
                phoneOrEmail = fullPhoneSentTo.ifEmpty { userPhone },
                otp = otp,
                onOtpChange = { otp = it },
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = { navController.popBackStack() },
                onResend = {
                    authViewModel.sendPhoneOtp(activity, userPhone)
                },
                onVerify = {
                    authViewModel.clearError()
                    authViewModel.verifyPhoneOtp(currentVerificationId, otp)
                }
            )
        }
        
        // ── EMAIL FLOW ──────────────────────────────────────────────────────────
        composable("email_auth") {
            EmailAuthScreen(
                initialIsSignup = initialRoute == "email_signup",
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = onBackToOnboarding,
                onLogin = { email, password ->
                    userEmail = email
                    userPassword = password
                    authViewModel.clearError()
                    authViewModel.loginWithEmail(email, password)
                },
                onSignup = { name, email, password ->
                    userEmail = email
                    userPassword = password
                    authViewModel.clearError()
                    authViewModel.registerWithEmail(email, password, name)
                },
                onForgotPassword = { email ->
                    authViewModel.clearError()
                    if (email.isBlank()) {
                        authViewModel.resetState() // Or show error to enter email
                    } else {
                        authViewModel.sendPasswordResetEmail(email)
                    }
                }
            )
        }
        
        composable("email_verification") {
            InputScreen(
                title = "Check your inbox",
                subtitle = "We sent a verification email to $userEmail.",
                value = "",
                onValueChange = {},
                placeholder = "",
                buttonText = "I verified it, Log in",
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = { navController.popBackStack("email_auth", false) },
                onContinue = {
                    authViewModel.clearError()
                    authViewModel.loginWithEmail(userEmail, userPassword)
                }
            )
        }
        
        // ── GOOGLE FLOW ─────────────────────────────────────────────────────────
        composable("google_loading") {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (errorMessage != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage, color = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBackToOnboarding) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
        

    }

}
