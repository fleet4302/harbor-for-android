package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.model.HarborThemeStyle

val LocalHarborTheme = staticCompositionLocalOf { HarborThemeStyle.DEEP_HARBOR }

@Composable
fun HarborTheme(
    themeStyle: HarborThemeStyle = HarborThemeStyle.DEEP_HARBOR,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = themeStyle.primary,
        onPrimary = Color.Black,
        primaryContainer = themeStyle.surfaceVariant,
        onPrimaryContainer = themeStyle.primary,
        secondary = themeStyle.secondary,
        onSecondary = Color.White,
        secondaryContainer = themeStyle.surfaceVariant,
        onSecondaryContainer = themeStyle.secondary,
        tertiary = themeStyle.accentGlow,
        background = themeStyle.background,
        onBackground = HarborTextPrimary,
        surface = themeStyle.surface,
        onSurface = HarborTextPrimary,
        surfaceVariant = themeStyle.surfaceVariant,
        onSurfaceVariant = HarborTextSecondary,
        outline = HarborBorder,
        outlineVariant = themeStyle.surfaceVariant
    )

    CompositionLocalProvider(LocalHarborTheme provides themeStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
