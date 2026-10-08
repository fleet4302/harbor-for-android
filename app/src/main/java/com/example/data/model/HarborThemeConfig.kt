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
    BLACK_PASTEL_WHITE(
        title = "Black & Pastel White",
        subtitle = "Pure Pitch Black & Soft Pastel White Accent",
        background = Color(0xFF000000),
        surface = Color(0xFF0E0E10),
        surfaceVariant = Color(0xFF1A1A1E),
        primary = Color(0xFFF3F4F6),
        secondary = Color(0xFFE5E7EB),
        accentGlow = Color(0xFFFAFAFA)
    ),
    BLACK_PASTEL_GREY(
        title = "Black & Light Pastel Grey",
        subtitle = "Pure Pitch Black & Light Pastel Grey Accent",
        background = Color(0xFF000000),
        surface = Color(0xFF0E0E10),
        surfaceVariant = Color(0xFF18181B),
        primary = Color(0xFFD1D5DB),
        secondary = Color(0xFF9CA3AF),
        accentGlow = Color(0xFFE2E8F0)
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
    ),
    CRIMSON_NEON(
        title = "Crimson Velvet",
        subtitle = "Pure Pitch Black & Radiant Crimson",
        background = Color(0xFF000000),
        surface = Color(0xFF0F0E11),
        surfaceVariant = Color(0xFF1B1820),
        primary = Color(0xFFFF2D55),
        secondary = Color(0xFFFB7185),
        accentGlow = Color(0xFFFF3366)
    )
}
