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
    LOVE_ROSE(
        title = "Love Velvet",
        subtitle = "Signature Obsidian & Radiant Heart Rose",
        background = Color(0xFF090A0F),
        surface = Color(0xFF12131C),
        surfaceVariant = Color(0xFF1D1F2D),
        primary = Color(0xFFFF2D55),
        secondary = Color(0xFFFB7185),
        accentGlow = Color(0xFFFF3366)
    ),
    ABYSS(
        title = "Obsidian Glow",
        subtitle = "Pure Pitch Black & Cyber Violet",
        background = Color(0xFF000000),
        surface = Color(0xFF0D0D12),
        surfaceVariant = Color(0xFF171720),
        primary = Color(0xFFA855F7),
        secondary = Color(0xFFEC4899),
        accentGlow = Color(0xFFA855F7)
    ),
    DEEP_HARBOR(
        title = "Harbor Dark",
        subtitle = "Pure Black & Harbor Slate Cyan",
        background = Color(0xFF000000),
        surface = Color(0xFF10121A),
        surfaceVariant = Color(0xFF1A1D28),
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFF38BDF8),
        accentGlow = Color(0xFF00E5FF)
    ),
    NAUTICAL_GOLD(
        title = "Sunset Gold",
        subtitle = "Dark Slate Navy & Warm Amber",
        background = Color(0xFF0B131E),
        surface = Color(0xFF131E2E),
        surfaceVariant = Color(0xFF1D2C42),
        primary = Color(0xFFF59E0B),
        secondary = Color(0xFFFB923C),
        accentGlow = Color(0xFFF59E0B)
    )
}
