package com.hereapp.messenger.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hereapp.messenger.core.voice.RecordedVoiceNote
import com.hereapp.messenger.core.voice.VoiceAudioController
import com.hereapp.messenger.ui.theme.LocalHereLuxuryColors
import kotlinx.coroutines.delay

@Composable
fun VoiceNoteStudioCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val luxuryColors = LocalHereLuxuryColors.current
    val controller = remember { VoiceAudioController(context) }

    var isRecording by remember { mutableStateOf(false) }
    var isRecordingPaused by remember { mutableStateOf(false) }
    var recordedNote by remember { mutableStateOf<RecordedVoiceNote?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val waveform = remember { mutableStateListOf<Float>() }

    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }

    LaunchedEffect(isRecording, isRecordingPaused) {
        while (isRecording && !isRecordingPaused) {
            val amp = controller.sampleNormalizedAmplitude()
            waveform.add(amp)
            if (waveform.size > 48) waveform.removeAt(0)
            delay(100L)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            waveform.clear()
            controller.startRecording()
                .onSuccess {
                    isRecording = true
                    isRecordingPaused = false
                    recordedNote = null
                    statusMessage = "Recording voice note..."
                }
                .onFailure { statusMessage = it.localizedMessage ?: "Unable to start recording." }
        } else {
            statusMessage = "Microphone permission is required for Here Voice."
        }
    }

    HereSurfaceCard(modifier = modifier) {
        Text(
            text = "Here Voice",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = statusMessage ?: "Record, preview waveform, pause/resume, and test playback speeds.",
            style = MaterialTheme.typography.bodySmall,
            color = luxuryColors.mutedText
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Live Audio Waveform Canvas
        val activeBars = if (isRecording) waveform.toList() else recordedNote?.waveform.orEmpty()
        val barColor = MaterialTheme.colorScheme.primary
        val idleColor = luxuryColors.hairlineBorder
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            val count = 36
            val step = size.width / count
            val centerY = size.height / 2f
            for (i in 0 until count) {
                val amp = activeBars.getOrNull(i) ?: 0.14f
                val halfHeight = (amp * size.height * 0.45f).coerceAtLeast(3f)
                val x = i * step + step / 2f
                drawLine(
                    color = if (activeBars.isNotEmpty()) barColor else idleColor,
                    start = Offset(x, centerY - halfHeight),
                    end = Offset(x, centerY + halfHeight),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isRecording) {
                IconButton(
                    onClick = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            waveform.clear()
                            controller.startRecording()
                                .onSuccess {
                                    isRecording = true
                                    isRecordingPaused = false
                                    recordedNote = null
                                    statusMessage = "Recording voice note..."
                                }
                                .onFailure { statusMessage = it.localizedMessage }
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("voice_record_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Record voice message",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        if (isRecordingPaused) {
                            controller.resumeRecording()
                            isRecordingPaused = false
                            statusMessage = "Recording resumed..."
                        } else {
                            controller.pauseRecording()
                            isRecordingPaused = true
                            statusMessage = "Recording paused"
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .testTag("voice_pause_record_button")
                ) {
                    Icon(
                        imageVector = if (isRecordingPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (isRecordingPaused) "Resume recording" else "Pause recording",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        val note = controller.stopRecording(waveform.toList())
                        isRecording = false
                        isRecordingPaused = false
                        recordedNote = note
                        statusMessage = if (note != null) {
                            "Recorded (${note.durationMs / 1000}s). Cloud upload requires Firebase Storage."
                        } else {
                            "Recording stopped."
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("voice_stop_record_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop recording",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            if (recordedNote != null && !isRecording) {
                IconButton(
                    onClick = {
                        val note = recordedNote ?: return@IconButton
                        if (isPlaying) {
                            controller.pausePlayback()
                            isPlaying = false
                        } else {
                            controller.startPlayback(note.filePath, playbackSpeed) {
                                isPlaying = false
                            }.onSuccess {
                                isPlaying = true
                            }.onFailure {
                                statusMessage = it.localizedMessage
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .testTag("voice_play_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause playback" else "Play voice note",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Playback speed options: 1.0x, 1.5x, 2.0x
                listOf(1.0f, 1.5f, 2.0f).forEach { speed ->
                    FilterChip(
                        selected = playbackSpeed == speed,
                        onClick = {
                            playbackSpeed = speed
                            controller.setPlaybackSpeed(speed)
                        },
                        label = { Text("${speed}x") },
                        modifier = Modifier.testTag("voice_speed_${speed}")
                    )
                }
            }
        }
    }
}
