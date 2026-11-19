package com.hereapp.messenger.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereapp.messenger.R
import com.hereapp.messenger.core.i18n.LocalHereStrings
import com.hereapp.messenger.core.i18n.SupportedLanguage
import com.hereapp.messenger.ui.components.HerePrimaryButton
import com.hereapp.messenger.ui.components.HereSecondaryButton
import com.hereapp.messenger.ui.components.LanguagePillButton
import com.hereapp.messenger.ui.theme.LocalHereLuxuryColors

@Composable
fun WelcomeScreen(
    currentLanguage: SupportedLanguage,
    onOpenLanguageSelector: () -> Unit,
    onSignInClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onExploreWorkspaceClick: () -> Unit
) {
    val strings = LocalHereStrings.current
    val luxuryColors = LocalHereLuxuryColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .widthIn(max = 560.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.appName.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                LanguagePillButton(
                    currentLanguage = currentLanguage,
                    onClick = onOpenLanguageSelector
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Architectural Black Luxury Hero Artwork
            Surface(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, luxuryColors.hairlineBorder),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_welcome_hero_1795082510566),
                        contentDescription = strings.appName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // HereApp Logo Emblem
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.height(72.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hereapp_icon_1795082505912),
                    contentDescription = "${strings.appName} Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.appName,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.slogan,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 3.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = strings.welcomeSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            HerePrimaryButton(
                text = strings.signIn,
                onClick = onSignInClick,
                testTag = "welcome_sign_in_button"
            )

            Spacer(modifier = Modifier.height(12.dp))

            HereSecondaryButton(
                text = strings.createAccount,
                onClick = onCreateAccountClick,
                testTag = "welcome_create_account_button"
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onExploreWorkspaceClick,
                modifier = Modifier.testTag("welcome_explore_workspace_button")
            ) {
                Text(
                    text = strings.exploreWorkspace,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
