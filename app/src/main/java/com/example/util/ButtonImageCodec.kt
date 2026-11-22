package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

    private const val MAX_DIM_PX = 144
    private const val MAX_ENCODED_BYTES = 24 * 1024

    suspend fun encodeUriToBase64(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                } ?: throw IllegalArgumentException("Could not open selected image")

                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    throw IllegalArgumentException("Selected file is not a valid image")
                }

                val sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIM_PX * 2)
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                } ?: throw IllegalArgumentException("Failed to decode image")

                val squareCropped = centerCropToSquare(decoded)
                if (squareCropped !== decoded) decoded.recycle()

                val scaled = if (squareCropped.width > MAX_DIM_PX || squareCropped.height > MAX_DIM_PX) {
                    Bitmap.createScaledBitmap(squareCropped, MAX_DIM_PX, MAX_DIM_PX, true).also {
                        if (it !== squareCropped) squareCropped.recycle()
                    }
                } else {
                    squareCropped
                }

                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }

                var quality = 78
                var bytes: ByteArray
                do {
                    val out = ByteArrayOutputStream()
                    scaled.compress(format, quality, out)
                    bytes = out.toByteArray()
                    quality -= 12
                } while (bytes.size > MAX_ENCODED_BYTES && quality >= 30)

                scaled.recycle()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        }

    fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val raw = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(raw, 0, raw.size)?.asImageBitmap()
        }.getOrNull()
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
}
