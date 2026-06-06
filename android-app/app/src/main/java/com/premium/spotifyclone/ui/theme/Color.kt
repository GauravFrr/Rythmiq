package com.premium.spotifyclone.ui.theme

import androidx.compose.ui.graphics.Color

// ── Rythmiq Design System ──────────────────────────────────────────────────
val AppBlack      = Color(0xFF000000)   // Pure black — all backgrounds
val AppSurface    = Color(0xFF111111)   // Song rows, mini-player
val AppElevated   = Color(0xFF1C1C1C)   // Search bar, tab bar, sheets
val AppDivider    = Color(0xFF222222)   // Row separators

val AccentRed     = Color(0xFFE8133A)   // Active tab, pause button, pills
val AccentRedDim  = Color(0x33E8133A)   // Red tint backgrounds

val TextPrimary   = Color(0xFFFFFFFF)   // Song titles, headers
val TextSecondary = Color(0xFF888888)   // Artist names, duration
val TextTertiary  = Color(0xFF444444)   // Inactive tab labels, hints

val IconDefault   = Color(0xFFAAAAAA)   // Action icons (♡ ⋮ etc)
val IconActive    = Color(0xFFFFFFFF)   // Active/highlighted icons

// Legacy aliases — keep so old files still compile
val PremiumBlack    = AppBlack
val PremiumCharcoal = AppSurface
val DeepSilver      = TextSecondary
val BrightSilver    = TextPrimary
val NeonAccent      = AccentRed          // was Spotify green
val GradientStart   = AppElevated
val GradientEnd     = AppBlack
val White           = Color.White
val ErrorRed        = Color(0xFFCF4040)
