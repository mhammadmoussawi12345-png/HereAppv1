package com.hereapp.messenger.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class HereAppAccent(
    val id: String,
    val displayName: String,
    val swatch: Color,
    val container: Color,
    val lightSwatch: Color = swatch,
    val lightContainer: Color = container
) {
    CHAMPAGNE_GOLD(
        id = "champagne_gold",
        displayName = "Champagne Gold",
        swatch = ChampagneGold,
        container = ChampagneGoldContainer,
        lightSwatch = Color(0xFF8A6F32),
        lightContainer = Color(0xFFEBE2CE)
    ),
    MIDNIGHT_NAVY(
        id = "midnight_navy",
        displayName = "Midnight Navy",
        swatch = Color(0xFF5D7EA8),
        container = Color(0xFF1A2538),
        lightSwatch = Color(0xFF2B4366),
        lightContainer = Color(0xFFD9E1EC)
    ),
    ROYAL_BROWN(
        id = "royal_brown",
        displayName = "Royal Brown",
        swatch = Color(0xFFA68265),
        container = Color(0xFF2B2018),
        lightSwatch = Color(0xFF6B4C35),
        lightContainer = Color(0xFFE9DFD6)
    ),
    DEEP_EMERALD(
        id = "deep_emerald",
        displayName = "Deep Emerald",
        swatch = Color(0xFF5B9E7E),
        container = Color(0xFF152920),
        lightSwatch = Color(0xFF256347),
        lightContainer = Color(0xFFD5E8DE)
    ),
    STERLING_SILVER(
        id = "sterling_silver",
        displayName = "Soft Silver",
        swatch = SterlingSilver,
        container = SterlingSilverContainer,
        lightSwatch = Color(0xFF5B616A),
        lightContainer = Color(0xFFE3E5E8)
    ),
    ROSE(
        id = "rose",
        displayName = "Dusty Rose",
        swatch = Color(0xFFCC8D98),
        container = Color(0xFF311F23),
        lightSwatch = Color(0xFF8C4E5A),
        lightContainer = Color(0xFFF0DDE1)
    ),
    ROYAL_PURPLE(
        id = "royal_purple",
        displayName = "Muted Lavender",
        swatch = Color(0xFFA490C4),
        container = Color(0xFF281F36),
        lightSwatch = Color(0xFF624D85),
        lightContainer = Color(0xFFE5DFF0)
    ),
    SLATE_GRAY(
        id = "slate_gray",
        displayName = "Slate Gray",
        swatch = Color(0xFF8B96A3),
        container = Color(0xFF1F242B),
        lightSwatch = Color(0xFF4D5763),
        lightContainer = Color(0xFFDFE3E8)
    ),
    SAGE_GREEN(
        id = "sage_green",
        displayName = "Olive Green",
        swatch = Color(0xFF8A9A68),
        container = Color(0xFF222819),
        lightSwatch = Color(0xFF536136),
        lightContainer = Color(0xFFE2E6D8)
    ),
    TERRACOTTA(
        id = "terracotta",
        displayName = "Terracotta",
        swatch = Color(0xFFC47D64),
        container = Color(0xFF301E18),
        lightSwatch = Color(0xFF8F4C35),
        lightContainer = Color(0xFFEFDFD9)
    ),
    DEEP_PLUM(
        id = "deep_plum",
        displayName = "Deep Plum",
        swatch = Color(0xFFA07498),
        container = Color(0xFF2B1C28),
        lightSwatch = Color(0xFF6B3E62),
        lightContainer = Color(0xFFEADCE7)
    ),
    SOFT_BLUE(
        id = "soft_blue",
        displayName = "Steel Blue",
        swatch = Color(0xFF7298B8),
        container = Color(0xFF1A2836),
        lightSwatch = Color(0xFF3B6282),
        lightContainer = Color(0xFFD9E5EF)
    ),
    WARM_SAND(
        id = "warm_sand",
        displayName = "Warm Beige",
        swatch = Color(0xFFCBB89D),
        container = Color(0xFF2E2820),
        lightSwatch = Color(0xFF7D694D),
        lightContainer = Color(0xFFEBE3D7)
    ),
    SOFT_BLACK(
        id = "soft_black",
        displayName = "Charcoal",
        swatch = Color(0xFF8E9198),
        container = Color(0xFF1F2024),
        lightSwatch = Color(0xFF3A3D44),
        lightContainer = Color(0xFFE0E1E4)
    ),
    PEARL_WHITE(
        id = "pearl_white",
        displayName = "Pearl White",
        swatch = Color(0xFFE2DFD8),
        container = Color(0xFF2A2926),
        lightSwatch = Color(0xFF6E6B64),
        lightContainer = Color(0xFFEBE9E4)
    ),
    OCEAN_TEAL(
        id = "ocean_teal",
        displayName = "Ocean Teal",
        swatch = Color(0xFF62ABA6),
        container = Color(0xFF172C2B),
        lightSwatch = Color(0xFF2C6E6A),
        lightContainer = Color(0xFFD6EAE8)
    ),
    MUTED_BLUE(
        id = "muted_blue",
        displayName = "Muted Blue",
        swatch = MutedBlue,
        container = MutedBlueContainer,
        lightSwatch = Color(0xFF445D78),
        lightContainer = Color(0xFFDCE4EC)
    );

    companion object {
        fun fromId(id: String?): HereAppAccent =
            entries.firstOrNull { it.id == id } ?: CHAMPAGNE_GOLD

        val luxuryFifteenPalette: List<HereAppAccent> = listOf(
            CHAMPAGNE_GOLD,
            MIDNIGHT_NAVY,
            ROYAL_BROWN,
            DEEP_EMERALD,
            STERLING_SILVER,
            ROSE,
            ROYAL_PURPLE,
            SLATE_GRAY,
            SAGE_GREEN,
            TERRACOTTA,
            DEEP_PLUM,
            SOFT_BLUE,
            WARM_SAND,
            SOFT_BLACK,
            PEARL_WHITE
        )

        val comfortablePalette: List<HereAppAccent> = luxuryFifteenPalette
    }
}

data class HereThemePreset(
    val id: String,
    val title: String,
    val isDarkMode: Boolean,
    val primary: HereAppAccent,
    val secondary: HereAppAccent
)

object HereThemePresets {
    val all: List<HereThemePreset> = listOf(
        HereThemePreset("black_gold", "Black & Gold", true, HereAppAccent.CHAMPAGNE_GOLD, HereAppAccent.WARM_SAND),
        HereThemePreset("midnight_calm", "Midnight Navy", true, HereAppAccent.MIDNIGHT_NAVY, HereAppAccent.SOFT_BLUE),
        HereThemePreset("royal_velvet", "Royal Purple", true, HereAppAccent.ROYAL_PURPLE, HereAppAccent.ROSE),
        HereThemePreset("ocean_sage", "Ocean & Sage", true, HereAppAccent.OCEAN_TEAL, HereAppAccent.SAGE_GREEN),
        HereThemePreset("warm_day", "Warm Sand Day", false, HereAppAccent.WARM_SAND, HereAppAccent.MIDNIGHT_NAVY),
        HereThemePreset("soft_breeze", "Soft Blue Day", false, HereAppAccent.SOFT_BLUE, HereAppAccent.SAGE_GREEN)
    )
}

@Immutable
data class HereLuxuryColors(
    val hairlineBorder: Color = HairlineBorder,
    val subtleDivider: Color = SubtleDivider,
    val mutedText: Color = MutedText,
    val success: Color = LuxurySuccess,
    val warning: Color = LuxuryWarning,
    val champagneGold: Color = ChampagneGold,
    val sterlingSilver: Color = SterlingSilver,
    val mutedBlue: Color = MutedBlue
)

val LocalHereLuxuryColors = staticCompositionLocalOf { HereLuxuryColors() }

private fun buildDarkColorScheme(
    accent: HereAppAccent,
    secondaryAccent: HereAppAccent
): ColorScheme {
    val primary = accent.swatch
    val onPrimary = Color(0xFF121316)
    val primaryContainer = accent.container
    val secondary = secondaryAccent.swatch
    val tertiary = if (accent == HereAppAccent.SOFT_BLUE) ChampagneGold else HereAppAccent.SOFT_BLUE.swatch

    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = onPrimary,
        secondaryContainer = secondaryAccent.container,
        onSecondaryContainer = WarmIvory,
        tertiary = tertiary,
        onTertiary = onPrimary,
        tertiaryContainer = MutedBlueContainer,
        onTertiaryContainer = WarmIvory,
        background = MatteBlack,
        onBackground = WarmIvory,
        surface = ObsidianSurface,
        onSurface = WarmIvory,
        surfaceVariant = GraphiteSurface,
        onSurfaceVariant = SoftSilverText,
        surfaceContainerLowest = MatteBlack,
        surfaceContainerLow = ObsidianSurface,
        surfaceContainer = GraphiteSurface,
        surfaceContainerHigh = CharcoalCard,
        surfaceContainerHighest = ElevatedSlate,
        outline = HairlineBorder,
        outlineVariant = SubtleDivider,
        error = LuxuryError,
        onError = MatteBlack
    )
}

private fun buildComfortableLightColorScheme(
    accent: HereAppAccent,
    secondaryAccent: HereAppAccent
): ColorScheme {
    // Eye-friendly warm alabaster/sand light surfaces (never harsh #FFFFFF)
    val warmBg = Color(0xFFF2EFE9)
    val warmSurface = Color(0xFFF8F6F1)
    val warmCard = Color(0xFFEBE6DD)
    val warmCardHigh = Color(0xFFE3DDD2)
    val charcoalText = Color(0xFF1F2126)
    val mutedInk = Color(0xFF5C6069)
    val lightBorder = Color(0xFFD5CFC4)

    return lightColorScheme(
        primary = accent.lightSwatch,
        onPrimary = Color(0xFFF8F6F1),
        primaryContainer = accent.lightContainer,
        onPrimaryContainer = charcoalText,
        secondary = secondaryAccent.lightSwatch,
        onSecondary = Color(0xFFF8F6F1),
        secondaryContainer = secondaryAccent.lightContainer,
        onSecondaryContainer = charcoalText,
        tertiary = HereAppAccent.OCEAN_TEAL.lightSwatch,
        onTertiary = Color(0xFFF8F6F1),
        background = warmBg,
        onBackground = charcoalText,
        surface = warmSurface,
        onSurface = charcoalText,
        surfaceVariant = warmCard,
        onSurfaceVariant = mutedInk,
        surfaceContainerLowest = warmSurface,
        surfaceContainerLow = warmBg,
        surfaceContainer = warmCard,
        surfaceContainerHigh = warmCardHigh,
        surfaceContainerHighest = Color(0xFFDAD3C7),
        outline = lightBorder,
        outlineVariant = Color(0xFFE2DDD3),
        error = Color(0xFFB33A3A),
        onError = Color(0xFFF8F6F1)
    )
}

@Composable
fun HereAppTheme(
    accent: HereAppAccent = HereAppAccent.CHAMPAGNE_GOLD,
    secondaryAccent: HereAppAccent = HereAppAccent.WARM_SAND,
    isDarkMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkMode) {
        buildDarkColorScheme(accent, secondaryAccent)
    } else {
        buildComfortableLightColorScheme(accent, secondaryAccent)
    }
    val luxuryColors = if (isDarkMode) {
        HereLuxuryColors()
    } else {
        HereLuxuryColors(
            hairlineBorder = Color(0xFFD5CFC4),
            subtleDivider = Color(0xFFE2DDD3),
            mutedText = Color(0xFF5C6069)
        )
    }
    CompositionLocalProvider(LocalHereLuxuryColors provides luxuryColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    HereAppTheme(content = content)
}
