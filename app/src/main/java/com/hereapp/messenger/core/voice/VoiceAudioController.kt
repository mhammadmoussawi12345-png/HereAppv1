package com.hereapp.messenger.core.voice

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

data class RecordedVoiceNote(
    val filePath: String,
    val durationMs: Long,
    val waveform: List<Float>
)

/**
 * Real Android MediaRecorder + MediaPlayer controller for Here Voice messages.
 * Supports recording, pause/resume, live amplitude sampling for waveform visualization,
 * and playback with variable speed (1.0x, 1.5x, 2.0x).
 */
class VoiceAudioController(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var activeFilePath: String? = null
    private var recordingStartMs: Long = 0L

    fun startRecording(): Result<String> = runCatching {
        stopRecordingInternal()
        val outputFile = File(context.cacheDir, "here_voice_${System.currentTimeMillis()}.m4a")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(96000)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        activeFilePath = outputFile.absolutePath
        recordingStartMs = System.currentTimeMillis()
        outputFile.absolutePath
    }

    fun sampleNormalizedAmplitude(): Float {
        val raw = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        return (raw / 32767f).coerceIn(0.08f, 1.0f)
    }

    fun pauseRecording(): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder?.pause()
            true
        } else {
            false
        }
    }.getOrDefault(false)

    fun resumeRecording(): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder?.resume()
            true
        } else {
            false
        }
    }.getOrDefault(false)

    fun stopRecording(waveform: List<Float>): RecordedVoiceNote? {
        val path = activeFilePath ?: return null
        val duration = (System.currentTimeMillis() - recordingStartMs).coerceAtLeast(500L)
        stopRecordingInternal()
        return RecordedVoiceNote(
            filePath = path,
            durationMs = duration,
            waveform = waveform.ifEmpty { listOf(0.2f, 0.5f, 0.35f, 0.7f, 0.4f) }
        )
    }

    private fun stopRecordingInternal() {
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
        activeFilePath = null
    }

    fun startPlayback(
        filePath: String,
        speed: Float = 1.0f,
        onCompletion: () -> Unit
    ): Result<Unit> = runCatching {
        stopPlayback()
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                playbackParams = playbackParams.setSpeed(speed)
            }
            setOnCompletionListener { onCompletion() }
            start()
        }
        player = mediaPlayer
    }

    fun pausePlayback() {
        runCatching {
            if (player?.isPlaying == true) {
                player?.pause()
            }
        }
    }

    fun resumePlayback() {
        runCatching {
            player?.start()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                player?.let { mp ->
                    val wasPlaying = mp.isPlaying
                    mp.playbackParams = mp.playbackParams.setSpeed(speed)
                    if (!wasPlaying) mp.pause()
                }
            }
        }
    }

    fun stopPlayback() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
    }

    fun release() {
        stopRecordingInternal()
        stopPlayback()
    }
}
