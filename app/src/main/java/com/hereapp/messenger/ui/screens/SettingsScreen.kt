package com.hereapp.messenger.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.hereapp.messenger.core.i18n.LocalHereStrings
import com.hereapp.messenger.core.i18n.SupportedLanguage
import com.hereapp.messenger.ui.components.HereSecondaryButton
import com.hereapp.messenger.ui.components.HereSectionHeader
import com.hereapp.messenger.ui.components.HereSurfaceCard
import com.hereapp.messenger.ui.theme.HereAppAccent
import com.hereapp.messenger.ui.theme.LocalHereLuxuryColors

@Composable
fun SettingsScreen(
    currentLanguage: SupportedLanguage,
    selectedAccent: HereAppAccent,
    secondaryAccent: HereAppAccent = HereAppAccent.WARM_SAND,
    isDarkMode: Boolean = true,
    onSelectAccent: (HereAppAccent) -> Unit,
    onApplyTheme: (isDarkMode: Boolean, primary: HereAppAccent, secondary: HereAppAccent) -> Unit = { _, primary, _ ->
        onSelectAccent(primary)
    },
    onOpenLanguageSelector: () -> Unit,
    onOpenControlCenter: () -> Unit,
    onReturnToWelcome: () -> Unit,
    onSignOut: () -> Unit = onReturnToWelcome,
    modifier: Modifier = Modifier
) {
    val strings = LocalHereStrings.current
    val luxuryColors = LocalHereLuxuryColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = strings.settingsTitle,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 1. Theme (Dark / Light)
        HereSectionHeader(title = strings.sectionAppearance)
        HereSurfaceCard {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = isDarkMode,
                    onClick = { onApplyTheme(true, selectedAccent, secondaryAccent) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DarkMode,
                            contentDescription = "Dark Mode",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Dark") },
                    modifier = Modifier.testTag("settings_mode_dark")
                )
                FilterChip(
                    selected = !isDarkMode,
                    onClick = { onApplyTheme(false, selectedAccent, secondaryAccent) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.LightMode,
                            contentDescription = "Light Mode",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Light") },
                    modifier = Modifier.testTag("settings_mode_light")
                )
            }
        }

        // 2. Color Theme (15 Luxurious & Calm Colors)
        HereSectionHeader(title = "COLOR THEME")
        HereSurfaceCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = "Color Theme",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Color Theme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HereAppAccent.luxuryFifteenPalette.forEach { accent ->
                    ColorThemeRow(
                        label = accent.displayName,
                        swatchColor = if (isDarkMode) accent.swatch else accent.lightSwatch,
                        selected = selectedAccent == accent,
                        onClick = { onSelectAccent(accent) },
                        testTag = "color_theme_${accent.id}"
                    )
                }
            }
        }

        // 3. Language
        HereSectionHeader(title = strings.sectionLanguage)
        HereSurfaceCard(
            onClick = onOpenLanguageSelector,
            modifier = Modifier.testTag("settings_language_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = strings.languageLabel,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.languageLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${currentLanguage.nativeName} · ${if (currentLanguage.isRtl) strings.rtlLabel else strings.ltrLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = luxuryColors.mutedText
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(
            color = luxuryColors.subtleDivider,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // 4. Sign Out
        HereSecondaryButton(
            text = "Sign Out",
            onClick = onSignOut,
            testTag = "settings_sign_out_button"
        )
    }
}

@Composable
private fun ColorThemeRow(
    label: String,
    swatchColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val luxuryColors = LocalHereLuxuryColors.current
    val shape = RoundedCornerShape(12.dp)
    Surface(
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else luxuryColors.hairlineBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
