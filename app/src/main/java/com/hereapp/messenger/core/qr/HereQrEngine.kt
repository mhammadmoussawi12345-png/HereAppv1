package com.hereapp.messenger.core.qr

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import com.hereapp.messenger.core.identity.IdentityConstants
import java.security.MessageDigest

sealed interface QrParseResult {
    data class Valid(val numericHereAppId: String, val canonicalLink: String) : QrParseResult
    data class Invalid(val reason: String) : QrParseResult
}

object HereQrEngine {
    private const val WEB_PREFIX = "https://hereapp.messenger/u/"
    private const val SCHEME_PREFIX = "hereapp://u/"

    fun buildProfileLink(numericHereAppId: String): String =
        "$WEB_PREFIX${numericHereAppId.trim()}"

    /**
     * Strictly validates a HereApp QR payload.
     * Only accepts opaque public numeric HereApp IDs (never passwords, tokens, or UIDs).
     */
    fun parse(rawPayload: String): QrParseResult {
        val trimmed = rawPayload.trim()
        val extractedId = when {
            trimmed.startsWith(WEB_PREFIX, ignoreCase = true) ->
                trimmed.substring(WEB_PREFIX.length).substringBefore("?").substringBefore("/")
            trimmed.startsWith(SCHEME_PREFIX, ignoreCase = true) ->
                trimmed.substring(SCHEME_PREFIX.length).substringBefore("?").substringBefore("/")
            else -> return QrParseResult.Invalid("Unsupported or modified QR code. Only official HereApp QR codes are accepted.")
        }

        if (!IdentityConstants.isValidNumericHereAppId(extractedId)) {
            return QrParseResult.Invalid("Invalid HereApp ID format in QR code.")
        }

        return QrParseResult.Valid(
            numericHereAppId = extractedId,
            canonicalLink = buildProfileLink(extractedId)
        )
    }

    /**
     * Generates a deterministic 25x25 QR-style matrix with standard 7x7 finder patterns
     * and SHA-256 derived data modules for visual rendering and bitmap export.
     */
    fun generateMatrix(payload: String, size: Int = 25): Array<BooleanArray> {
        val grid = Array(size) { BooleanArray(size) }

        fun drawFinder(top: Int, left: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    grid[top + r][left + c] = isBorder || isCenter
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray())
        var bitIndex = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= size - 8
                val inBottomLeft = r >= size - 8 && c < 8
                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                    val byteVal = digest[(bitIndex / 8) % digest.size].toInt()
                    val bit = (byteVal shr (bitIndex % 8)) and 1
                    grid[r][c] = bit == 1
                    bitIndex++
                }
            }
        }
        return grid
    }

    fun renderBlackGoldQrBitmap(payload: String, pixelSize: Int = 720): Bitmap {
        val matrix = generateMatrix(payload)
        val bitmap = Bitmap.createBitmap(pixelSize, pixelSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint().apply { color = 0xFF0A0A0C.toInt() }
        val fgPaint = Paint().apply { color = 0xFFD4AF6A.toInt() }
        canvas.drawRect(0f, 0f, pixelSize.toFloat(), pixelSize.toFloat(), bgPaint)

        val padding = pixelSize * 0.1f
        val cellSize = (pixelSize - padding * 2f) / matrix.size
        for (r in matrix.indices) {
            for (c in matrix[r].indices) {
                if (matrix[r][c]) {
                    val left = padding + c * cellSize
                    val top = padding + r * cellSize
                    canvas.drawRoundRect(
                        left,
                        top,
                        left + cellSize * 0.92f,
                        top + cellSize * 0.92f,
                        cellSize * 0.22f,
                        cellSize * 0.22f,
                        fgPaint
                    )
                }
            }
        }
        return bitmap
    }

    fun saveQrBitmapToGallery(context: Context, payload: String, fileName: String): Result<String> =
        runCatching {
            val bitmap = renderBlackGoldQrBitmap(payload)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HereApp")
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Unable to create image entry in gallery.")
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            } ?: throw IllegalStateException("Unable to write QR image.")
            "Saved to Pictures/HereApp/$fileName.png"
        }
}
