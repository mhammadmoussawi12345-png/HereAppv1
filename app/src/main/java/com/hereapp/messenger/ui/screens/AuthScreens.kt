package com.hereapp.messenger.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hereapp.messenger.core.i18n.LocalHereStrings
import com.hereapp.messenger.ui.auth.AuthUiState
import com.hereapp.messenger.ui.components.HerePrimaryButton
import com.hereapp.messenger.ui.components.HereSecondaryButton
import com.hereapp.messenger.ui.components.HereSectionHeader
import com.hereapp.messenger.ui.components.HereStatusBanner
import com.hereapp.messenger.ui.components.HereTextField
import com.hereapp.messenger.ui.theme.ChampagneGold
import com.hereapp.messenger.ui.theme.HereAppAccent
import com.hereapp.messenger.ui.theme.LocalHereLuxuryColors
import com.hereapp.messenger.ui.theme.MutedBlue
import com.hereapp.messenger.ui.theme.SterlingSilver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthScaffold(
    title: String,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val strings = LocalHereStrings.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("auth_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.back
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
                if (isLoading) {
                    androidx.compose.material3.LinearProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_loading_indicator")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SignInScreen(
    authState: AuthUiState,
    onClearFeedback: () -> Unit,
    onBack: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onForgotPassword: (String) -> Unit
) {
    val strings = LocalHereStrings.current
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { onClearFeedback() }

    AuthScaffold(title = strings.signIn, isLoading = authState.isLoading, onBack = onBack) {
        Text(
            text = strings.slogan,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        authState.feedbackMessage?.let { message ->
            HereStatusBanner(text = message)
        }

        HereTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = "Email or @username",
            leadingIcon = Icons.Outlined.Email,
            testTag = "sign_in_identifier_input"
        )

        HereTextField(
            value = password,
            onValueChange = { password = it },
            label = strings.passwordLabel,
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = PasswordVisualTransformation(),
            testTag = "sign_in_password_input"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    resetEmail = if (identifier.contains("@") && identifier.contains(".")) identifier else ""
                    showForgotDialog = true
                },
                modifier = Modifier.testTag("sign_in_forgot_password_button")
            ) {
                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        HerePrimaryButton(
            text = strings.signIn,
            onClick = { onSignIn(identifier, password) },
            enabled = !authState.isLoading,
            testTag = "sign_in_submit_button"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = LocalHereLuxuryColors.current.subtleDivider
            )
            Text(
                text = "OR",
                style = MaterialTheme.typography.labelSmall,
                color = LocalHereLuxuryColors.current.mutedText
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = LocalHereLuxuryColors.current.subtleDivider
            )
        }

        HereSecondaryButton(
            text = "Continue with Google",
            onClick = onGoogleSignIn,
            enabled = !authState.isLoading,
            testTag = "sign_in_google_button"
        )
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter the email associated with your HereApp account to receive a password recovery link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HereTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = strings.emailLabel,
                        leadingIcon = Icons.Outlined.Email,
                        testTag = "forgot_password_email_input"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForgotDialog = false
                        onForgotPassword(resetEmail)
                    },
                    modifier = Modifier.testTag("forgot_password_send_button")
                ) {
                    Text("Send Link", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text(strings.back, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun CreateAccountScreen(
    authState: AuthUiState,
    onClearFeedback: () -> Unit,
    onBack: () -> Unit,
    onCreateAccount: (email: String, password: String, confirmPassword: String, acceptedTerms: Boolean) -> Unit,
    onOpenProfileSetup: () -> Unit,
    onOpenSignIn: () -> Unit = {}
) {
    val strings = LocalHereStrings.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onClearFeedback() }

    AuthScaffold(title = strings.createAccount, isLoading = authState.isLoading, onBack = onBack) {
        authState.feedbackMessage?.let { message ->
            HereStatusBanner(text = message)
        }

        HereTextField(
            value = email,
            onValueChange = { email = it },
            label = strings.emailLabel,
            leadingIcon = Icons.Outlined.Email,
            testTag = "create_account_email_input"
        )

        HereTextField(
            value = password,
            onValueChange = { password = it },
            label = strings.passwordLabel,
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = PasswordVisualTransformation(),
            testTag = "create_account_password_input"
        )

        HereTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm password",
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = PasswordVisualTransformation(),
            testTag = "create_account_confirm_password_input"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { acceptedTerms = !acceptedTerms }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = acceptedTerms,
                onCheckedChange = { acceptedTerms = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("create_account_terms_checkbox")
            )
            Text(
                text = "I accept the Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, LocalHereLuxuryColors.current.hairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MarkEmailRead,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Email verification is required after registration before full account activation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        HerePrimaryButton(
            text = strings.createAccount,
            onClick = { onCreateAccount(email, password, confirmPassword, acceptedTerms) },
            enabled = !authState.isLoading,
            testTag = "create_account_submit_button"
        )

        HereSecondaryButton(
            text = "Already have an account? ${strings.signIn}",
            onClick = onOpenSignIn,
            testTag = "create_account_go_to_sign_in_button"
        )

        HereSecondaryButton(
            text = strings.profileSetup,
            onClick = onOpenProfileSetup,
            testTag = "create_account_profile_setup_button"
        )
    }
}

@Composable
fun ProfileSetupScreen(
    authState: AuthUiState,
    selectedAccent: HereAppAccent,
    onSelectAccent: (HereAppAccent) -> Unit,
    onClearFeedback: () -> Unit,
    onBack: () -> Unit,
    onSaveProfile: (
        username: String,
        displayName: String,
        biography: String,
        countryOrRegion: String,
        photoUri: String?,
        personalAccent: HereAppAccent
    ) -> Unit
) {
    val strings = LocalHereStrings.current
    val luxuryColors = LocalHereLuxuryColors.current
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var biography by remember { mutableStateOf("") }
    var countryOrRegion by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    LaunchedEffect(Unit) { onClearFeedback() }

    AuthScaffold(title = strings.profileSetup, isLoading = authState.isLoading, onBack = onBack) {
        authState.feedbackMessage?.let { message ->
            HereStatusBanner(text = message)
        }

        // 1. Profile Picture (Zero-permission Android Photo Picker)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                .padding(16.dp)
                .testTag("profile_setup_photo_picker"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddAPhoto,
                    contentDescription = "Select profile picture",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Profile Picture",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (selectedPhotoUri != null) {
                        "Photo selected"
                    } else {
                        "Tap to choose from gallery"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = luxuryColors.mutedText
                )
            }
        }

        // 2. Unique @Username
        HereTextField(
            value = username,
            onValueChange = { username = it },
            label = "Unique @Username",
            leadingIcon = Icons.Outlined.AlternateEmail,
            testTag = "profile_setup_username_input"
        )

        // 3. Display Name
        HereTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = "Display Name",
            leadingIcon = Icons.Outlined.Person,
            testTag = "profile_setup_display_name_input"
        )

        // 4. Biography
        HereTextField(
            value = biography,
            onValueChange = { biography = it },
            label = "Biography",
            singleLine = false,
            testTag = "profile_setup_biography_input"
        )

        // 5. Country or Region
        HereTextField(
            value = countryOrRegion,
            onValueChange = { countryOrRegion = it },
            label = "Country or Region",
            leadingIcon = Icons.Outlined.Public,
            testTag = "profile_setup_country_input"
        )

        // 6. Personal Theme Color
        HereSectionHeader(title = "PERSONAL THEME COLOR")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThemeSwatchChip(
                label = strings.accentGold,
                color = ChampagneGold,
                selected = selectedAccent == HereAppAccent.CHAMPAGNE_GOLD,
                onClick = { onSelectAccent(HereAppAccent.CHAMPAGNE_GOLD) },
                modifier = Modifier.weight(1f),
                testTag = "profile_theme_gold"
            )
            ThemeSwatchChip(
                label = strings.accentSilver,
                color = SterlingSilver,
                selected = selectedAccent == HereAppAccent.STERLING_SILVER,
                onClick = { onSelectAccent(HereAppAccent.STERLING_SILVER) },
                modifier = Modifier.weight(1f),
                testTag = "profile_theme_silver"
            )
            ThemeSwatchChip(
                label = strings.accentBlue,
                color = MutedBlue,
                selected = selectedAccent == HereAppAccent.MUTED_BLUE,
                onClick = { onSelectAccent(HereAppAccent.MUTED_BLUE) },
                modifier = Modifier.weight(1f),
                testTag = "profile_theme_blue"
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 7. Continue Button
        HerePrimaryButton(
            text = strings.continueAction,
            onClick = {
                onSaveProfile(
                    username,
                    displayName,
                    biography,
                    countryOrRegion,
                    selectedPhotoUri?.toString(),
                    selectedAccent
                )
            },
            enabled = !authState.isLoading,
            testTag = "profile_setup_continue_button"
        )
    }
}

@Composable
private fun ThemeSwatchChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val luxuryColors = LocalHereLuxuryColors.current
    val shape = RoundedCornerShape(12.dp)
    Surface(
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else luxuryColors.hairlineBorder),
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color(0xFF0A0A0C),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = label.substringBefore(" "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
