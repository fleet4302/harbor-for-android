package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class HarborThemeStyle(
    val title: String,
    val subtitle: String,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val secondary: Color,
    val accentGlow: Color
) {
    DEEP_HARBOR(
        title = "Deep Harbor",
        subtitle = "Signature Maritime Navy & Neon Cyan",
        background = Color(0xFF060D19),
        surface = Color(0xFF0C182B),
        surfaceVariant = Color(0xFF14243D),
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFF3B82F6),
        accentGlow = Color(0xFF00E5FF)
    ),
    ABYSS(
        title = "Abyss OLED",
        subtitle = "Pure Pitch Black & Cyber Violet",
        background = Color(0xFF000000),
        surface = Color(0xFF0D0D12),
        surfaceVariant = Color(0xFF171720),
        primary = Color(0xFFA855F7),
        secondary = Color(0xFFEC4899),
        accentGlow = Color(0xFFA855F7)
    ),
    NAUTICAL_GOLD(
        title = "Nautical Gold",
        subtitle = "Dark Slate Navy & Warm Amber",
        background = Color(0xFF0B131E),
        surface = Color(0xFF131E2E),
        surfaceVariant = Color(0xFF1D2C42),
        primary = Color(0xFFF59E0B),
        secondary = Color(0xFFFB923C),
        accentGlow = Color(0xFFF59E0B)
    ),
    NORDIC_EMERALD(
        title = "Nordic Fog",
        subtitle = "Charcoal Slate & Aurora Emerald",
        background = Color(0xFF0A1214),
        surface = Color(0xFF101E20),
        surfaceVariant = Color(0xFF182D30),
        primary = Color(0xFF10B981),
        secondary = Color(0xFF06B6D4),
        accentGlow = Color(0xFF10B981)
    )
}
