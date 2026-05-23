package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    tertiary = WinGreen,
    background = SlateDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color.White
)

private val LightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    tertiary = WinGreen,
    background = Color(0xFFF1F5F9), // Slate 100 for elegant, clean, crisp light background
    onBackground = Color(0xFF0B0E14),
    surface = Color.White,
    onSurface = Color(0xFF0B0E14),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF1E293B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // The default theme should be a light mode.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
