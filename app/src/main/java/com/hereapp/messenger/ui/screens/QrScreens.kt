package com.hereapp.messenger.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.hereapp.messenger.R
import com.hereapp.messenger.core.auth.AuthSession
import com.hereapp.messenger.core.qr.HereQrEngine
import com.hereapp.messenger.core.qr.QrParseResult
import com.hereapp.messenger.ui.components.HerePrimaryButton
import com.hereapp.messenger.ui.components.HereSecondaryButton
import com.hereapp.messenger.ui.components.HereStatusBanner
import com.hereapp.messenger.ui.components.HereSurfaceCard
import com.hereapp.messenger.ui.components.HereTextField
import com.hereapp.messenger.ui.theme.ChampagneGold
import com.hereapp.messenger.ui.theme.LocalHereLuxuryColors
import com.hereapp.messenger.ui.theme.MatteBlack
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQrCodeScreen(
    authSession: AuthSession?,
    onBack: () -> Unit,
    onOpenScanner: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val luxuryColors = LocalHereLuxuryColors.current
    val profile = authSession?.profile
    val hereAppId = profile?.hereAppId ?: "00000"
    val qrPayload = remember(hereAppId) { HereQrEngine.buildProfileLink(hereAppId) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var showFullScreen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("My QR Code", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("qr_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenScanner, modifier = Modifier.testTag("qr_open_scanner_button")) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scan QR")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            feedback?.let { HereStatusBanner(text = it) }

            HereSurfaceCard(modifier = Modifier.widthIn(max = 420.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = profile?.displayName?.ifBlank { null } ?: "HereApp Member",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "@${profile?.username?.ifBlank { "unclaimed" } ?: "unclaimed"} · ID $hereAppId",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChampagneGold
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    BlackGoldQrGraphic(
                        payload = qrPayload,
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { showFullScreen = true }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap QR code for full-screen display",
                        style = MaterialTheme.typography.labelSmall,
                        color = luxuryColors.mutedText
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HerePrimaryButton(
                    text = "Share QR",
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Connect with me on HereApp: $qrPayload")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share HereApp QR"))
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "qr_share_button"
                )
                HereSecondaryButton(
                    text = "Save Image",
                    onClick = {
                        HereQrEngine.saveQrBitmapToGallery(context, qrPayload, "HereApp_QR_$hereAppId")
                            .onSuccess { feedback = it }
                            .onFailure { feedback = it.localizedMessage ?: "Unable to save image." }
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "qr_save_image_button"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HereSecondaryButton(
                    text = "Copy Link",
                    onClick = {
                        clipboard.setText(AnnotatedString(qrPayload))
                        feedback = "Profile link copied: $qrPayload"
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "qr_copy_link_button"
                )
                HereSecondaryButton(
                    text = "Full Screen",
                    onClick = { showFullScreen = true },
                    modifier = Modifier.weight(1f),
                    testTag = "qr_fullscreen_button"
                )
            }
        }
    }

    if (showFullScreen) {
        AlertDialog(
            onDismissRequest = { showFullScreen = false },
            containerColor = MatteBlack,
            title = {
                Text(
                    text = "ID $hereAppId",
                    style = MaterialTheme.typography.titleLarge,
                    color = ChampagneGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                BlackGoldQrGraphic(
                    payload = qrPayload,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                )
            },
            confirmButton = {
                TextButton(onClick = { showFullScreen = false }) {
                    Text("Close", color = ChampagneGold)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrScreen(
    onBack: () -> Unit,
    onConfirmedStartChat: (targetUid: String, displayName: String, username: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val luxuryColors = LocalHereLuxuryColors.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraDeniedMessage by remember { mutableStateOf<String?>(null) }
    var scannedPayload by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var isLookingUp by remember { mutableStateOf(false) }
    var foundUser by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        cameraDeniedMessage = if (granted) null else "Camera permission was denied. Enable Camera access in Settings to scan QR codes."
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Scan HereApp QR", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("scan_qr_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            cameraDeniedMessage?.let { HereStatusBanner(text = it) }
            feedback?.let { HereStatusBanner(text = it) }

            HereSurfaceCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = if (hasCameraPermission) {
                            "Camera Scanner Ready"
                        } else {
                            "Camera Access Required"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (hasCameraPermission) {
                            "Point your camera at a HereApp QR code or validate a scanned HereApp QR link below."
                        } else {
                            "Grant camera permission to scan another member's HereApp QR code."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = luxuryColors.mutedText,
                        textAlign = TextAlign.Center
                    )
                    if (!hasCameraPermission) {
                        HerePrimaryButton(
                            text = "Allow Camera Access",
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            testTag = "scan_qr_request_camera_button"
                        )
                    }
                }
            }

            HereTextField(
                value = scannedPayload,
                onValueChange = { scannedPayload = it },
                label = "Scanned QR payload (e.g. https://hereapp.messenger/u/00001)",
                leadingIcon = Icons.Outlined.QrCodeScanner,
                testTag = "scan_qr_payload_input"
            )

            HerePrimaryButton(
                text = if (isLookingUp) "Validating..." else "Validate & Lookup Profile",
                enabled = !isLookingUp,
                onClick = {
                    feedback = null
                    foundUser = null
                    when (val parsed = HereQrEngine.parse(scannedPayload)) {
                        is QrParseResult.Invalid -> feedback = parsed.reason
                        is QrParseResult.Valid -> {
                            isLookingUp = true
                            scope.launch {
                                try {
                                    val db = FirebaseFirestore.getInstance(
                                        context.getString(R.string.firestore_database_id)
                                    )
                                    val idSnap = db.collection("hereapp_ids")
                                        .document(parsed.numericHereAppId).get().await()
                                    val uid = idSnap.getString("uid")
                                    if (!idSnap.exists() || uid.isNullOrBlank()) {
                                        feedback = "No active HereApp account found for ID ${parsed.numericHereAppId}."
                                    } else {
                                        val userSnap = db.collection("users").document(uid).get().await()
                                        val suspended = userSnap.getBoolean("suspended") == true
                                        if (!userSnap.exists() || suspended) {
                                            feedback = "This account is unavailable or suspended."
                                        } else {
                                            val displayName = userSnap.getString("displayName") ?: "HereApp User"
                                            val username = userSnap.getString("username") ?: parsed.numericHereAppId
                                            foundUser = Triple(uid, displayName, username)
                                        }
                                    }
                                } catch (e: Exception) {
                                    feedback = e.localizedMessage ?: "Unable to verify QR code."
                                } finally {
                                    isLookingUp = false
                                }
                            }
                        }
                    }
                },
                testTag = "scan_qr_validate_button"
            )

            foundUser?.let { (uid, displayName, username) ->
                HereSurfaceCard {
                    Text(displayName, style = MaterialTheme.typography.titleLarge)
                    Text("@$username", style = MaterialTheme.typography.bodySmall, color = ChampagneGold)
                    Spacer(modifier = Modifier.height(12.dp))
                    HerePrimaryButton(
                        text = "Confirm & Start Private Conversation",
                        onClick = { onConfirmedStartChat(uid, displayName, username) },
                        testTag = "scan_qr_confirm_chat_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun BlackGoldQrGraphic(
    payload: String,
    modifier: Modifier = Modifier
) {
    val matrix = remember(payload) { HereQrEngine.generateMatrix(payload) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MatteBlack,
        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val cell = size.width / matrix.size
            for (r in matrix.indices) {
                for (c in matrix[r].indices) {
                    if (matrix[r][c]) {
                        drawRoundRect(
                            color = ChampagneGold,
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell * 0.9f, cell * 0.9f),
                            cornerRadius = CornerRadius(cell * 0.22f, cell * 0.22f)
                        )
                    }
                }
            }
        }
    }
}
