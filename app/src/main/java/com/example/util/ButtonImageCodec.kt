package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ButtonImageCodec {

    private const val MAX_DIM_PX = 160
    private const val MAX_ENCODED_BYTES = 48 * 1024

    suspend fun encodeUriToBase64(
        context: Context,
        uri: Uri,
        cropSquare: Boolean = true,
        maxDimPx: Int = MAX_DIM_PX,
        maxEncodedBytes: Int = MAX_ENCODED_BYTES
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val decoded = decodeUriBitmap(context, uri, maxDimPx)
                ?: throw IllegalArgumentException("Unsupported or unreadable image file")
            try {
                encodeBitmapToBase64(
                    source = decoded,
                    cropSquare = cropSquare,
                    maxDimPx = maxDimPx,
                    maxEncodedBytes = maxEncodedBytes
                )
            } finally {
                if (!decoded.isRecycled) decoded.recycle()
            }
        }
    }

    fun encodeBitmapToBase64(
        source: Bitmap,
        cropSquare: Boolean = true,
        maxDimPx: Int = MAX_DIM_PX,
        maxEncodedBytes: Int = MAX_ENCODED_BYTES
    ): String {
        val softSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            source.config == Bitmap.Config.HARDWARE
        ) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            source
        }

        val shaped = if (cropSquare) centerCropToSquare(softSource) else softSource
        if (shaped !== softSource && softSource !== source) softSource.recycle()

        val scaled = scaleDownToFit(shaped, maxDimPx)
        if (scaled !== shaped && shaped !== source) shaped.recycle()

        val hasAlpha = scaled.hasAlpha()
        val primaryFormat = when {
            hasAlpha && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Bitmap.CompressFormat.WEBP_LOSSY
            hasAlpha -> Bitmap.CompressFormat.PNG
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Bitmap.CompressFormat.WEBP_LOSSY
            else -> Bitmap.CompressFormat.JPEG
        }

        var quality = 86
        var bytes = ByteArray(0)
        do {
            val out = ByteArrayOutputStream()
            val ok = scaled.compress(primaryFormat, quality, out)
            if (ok) {
                bytes = out.toByteArray()
            }
            quality -= 14
        } while (bytes.size > maxEncodedBytes && quality >= 24 && primaryFormat != Bitmap.CompressFormat.PNG)

        if (bytes.isEmpty()) {
            val pngOut = ByteArrayOutputStream()
            val fallbackOk = scaled.compress(Bitmap.CompressFormat.PNG, 100, pngOut)
            if (fallbackOk) {
                bytes = pngOut.toByteArray()
            }
        }

        if (scaled !== source && !scaled.isRecycled) scaled.recycle()
        if (bytes.isEmpty()) {
            throw IllegalStateException("Could not encode bitmap")
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val raw = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(raw, 0, raw.size)?.asImageBitmap()
        }.getOrNull()
    }

    private fun decodeUriBitmap(context: Context, uri: Uri, maxDimPx: Int = MAX_DIM_PX): Bitmap? {
        val resolver = context.contentResolver

        // Try ImageDecoder first on Android 9+ (handles HEIC/HEIF/AVIF/WebP/PNG/JPG + Photo Picker URIs cleanly)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val decoderResult = runCatching {
                val source = ImageDecoder.createSource(resolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width
                    val h = info.size.height
                    if (w > 0 && h > 0 && (w > maxDimPx * 2 || h > maxDimPx * 2)) {
                        val scale = (maxDimPx * 2f) / max(w, h).toFloat()
                        decoder.setTargetSize(
                            (w * scale).roundToInt().coerceAtLeast(1),
                            (h * scale).roundToInt().coerceAtLeast(1)
                        )
                    }
                }
            }.getOrNull()
            if (decoderResult != null) return decoderResult
        }

        // Single-stream read into memory (never opens one-shot Photo Picker URI twice!)
        val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
            }
            ?: return null

        if (rawBytes.isEmpty()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDimPx * 2)
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
    }

    private fun computeInSampleSize(width: Int, height: Int, targetMax: Int): Int {
        var inSampleSize = 1
        while ((width / inSampleSize) > targetMax * 2 || (height / inSampleSize) > targetMax * 2) {
            inSampleSize *= 2
        }
        return max(1, inSampleSize)
    }

    private fun centerCropToSquare(source: Bitmap): Bitmap {
        val side = min(source.width, source.height)
        if (source.width == source.height) return source
        val left = ((source.width - side) / 2f).roundToInt().coerceAtLeast(0)
        val top = ((source.height - side) / 2f).roundToInt().coerceAtLeast(0)
        return Bitmap.createBitmap(source, left, top, side, side)
    }

    private fun scaleDownToFit(source: Bitmap, maxDim: Int): Bitmap {
        val maxSide = max(source.width, source.height)
        if (maxSide <= maxDim) return source
        val ratio = maxDim.toFloat() / maxSide.toFloat()
        val newW = (source.width * ratio).roundToInt().coerceAtLeast(1)
        val newH = (source.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }
}
