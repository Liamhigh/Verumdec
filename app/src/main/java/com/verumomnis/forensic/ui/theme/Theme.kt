package com.verumomnis.forensic.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VerumDarkColorScheme = darkColorScheme(
    primary = VO_AccentBlue,
    onPrimary = VO_White,
    primaryContainer = VO_DeepBlue,
    onPrimaryContainer = VO_White,
    secondary = VO_Gray,
    onSecondary = VO_Black,
    background = VO_Black,
    onBackground = VO_White,
    surface = VO_Surface,
    onSurface = VO_White,
    surfaceVariant = VO_DarkGray,
    onSurfaceVariant = VO_Gray,
    error = VO_Red,
    onError = VO_White
)

@Composable
fun VerumTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = VerumDarkColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = VO_Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
