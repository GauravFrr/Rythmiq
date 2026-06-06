package com.premium.spotifyclone.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val RythmiqColorScheme = darkColorScheme(
    primary        = AccentRed,
    onPrimary      = TextPrimary,
    secondary      = AppSurface,
    onSecondary    = TextPrimary,
    tertiary       = TextSecondary,
    onTertiary     = TextPrimary,
    background     = AppBlack,
    onBackground   = TextPrimary,
    surface        = AppSurface,
    onSurface      = TextPrimary,
    surfaceVariant = AppElevated,
    error          = ErrorRed,
)

@Composable
fun SpotifyCloneTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = RythmiqColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor  = AppBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
