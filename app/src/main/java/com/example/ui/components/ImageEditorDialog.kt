package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ImageCropAspect(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    SQUARE("1:1", 1f),
    WIDE("16:9", 16f / 9f),
    CLASSIC("4:3", 4f / 3f),
    PORTRAIT("4:5", 4f / 5f)
}

enum class ImageFilterPreset(
    val label: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val warmTint: Float = 0f
) {
    ORIGINAL("Original", 0f, 1f, 1f, 0f),
    VIVID("Vivid", 8f, 1.18f, 1.38f, 0f),
    MOODY("Moody", -14f, 1.24f, 0.78f, -0.08f),
    BW("B&W", 4f, 1.22f, 0f, 0f),
    WARM("Warm", 6f, 1.06f, 1.12f, 0.14f),
    COOL("Cool", 4f, 1.08f, 1.06f, -0.14f)
}

data class ImageEditParams(
    val rotationDeg: Int = 0,
    val flipHorizontal: Boolean = false,
    val cropAspect: ImageCropAspect = ImageCropAspect.ORIGINAL,
    val brightness: Float = 0f,     // -80..80
    val contrast: Float = 1f,       // 0.5..1.8
    val saturation: Float = 1f,     // 0..2.0
    val warmTint: Float = 0f        // -0.25..0.25
)

object ImageEditProcessor {

    suspend fun loadEditableBitmap(context: Context, uri: Uri, maxEdge: Int = 1080): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        val w = info.size.width
                        val h = info.size.height
                        val m = max(w, h)
                        if (m > maxEdge) {
                            val s = maxEdge.toFloat() / m.toFloat()
                            decoder.setTargetSize(
                                (w * s).roundToInt().coerceAtLeast(1),
                                (h * s).roundToInt().coerceAtLeast(1)
                            )
                        }
                    }
                } else {
                    val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@runCatching null
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
                    var sample = 1
                    while (max(bounds.outWidth, bounds.outHeight) / sample > maxEdge * 2) {
                        sample *= 2
                    }
                    BitmapFactory.decodeByteArray(
                        raw,
                        0,
                        raw.size,
                        BitmapFactory.Options().apply {
                            inSampleSize = sample
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                    )
                }
            }.getOrNull()
        }

    fun applyEdits(source: Bitmap, params: ImageEditParams): Bitmap {
        val matrix = Matrix()
        if (params.rotationDeg % 360 != 0) {
            matrix.postRotate(params.rotationDeg.toFloat())
        }
        if (params.flipHorizontal) {
            matrix.postScale(-1f, 1f)
        }

        val transformed = if (!matrix.isIdentity) {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        val cropped = params.cropAspect.ratio?.let { targetRatio ->
            cropToAspect(transformed, targetRatio)
        } ?: transformed
        if (cropped !== transformed && transformed !== source) {
            transformed.recycle()
        }

        val out = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(buildColorMatrix(params))
        canvas.drawBitmap(cropped, 0f, 0f, paint)

        if (cropped !== source && cropped !== transformed) {
            cropped.recycle()
        } else if (transformed !== source && cropped === transformed) {
            transformed.recycle()
        }
        return out
    }

    suspend fun saveEditedBitmapToCacheUri(context: Context, bitmap: Bitmap): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "edited_uploads").apply { mkdirs() }
                // Clean old temp files beyond the newest 8
                dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(8)?.forEach { it.delete() }
                val file = File(dir, "edited_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                Uri.fromFile(file)
            }
        }

    private fun cropToAspect(src: Bitmap, ratio: Float): Bitmap {
        val w = src.width.toFloat()
        val h = src.height.toFloat()
        val current = w / h
        val targetW: Int
        val targetH: Int
        if (current > ratio) {
            targetH = src.height
            targetW = (h * ratio).roundToInt().coerceIn(1, src.width)
        } else {
            targetW = src.width
            targetH = (w / ratio).roundToInt().coerceIn(1, src.height)
        }
        val x = ((src.width - targetW) / 2).coerceAtLeast(0)
        val y = ((src.height - targetH) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(src, x, y, targetW, targetH)
    }

    private fun buildColorMatrix(params: ImageEditParams): ColorMatrix {
        val combined = ColorMatrix()

        // Saturation
        val sat = ColorMatrix().apply { setSaturation(params.saturation.coerceIn(0f, 2f)) }
        combined.postConcat(sat)

        // Contrast & Brightness
        val c = params.contrast.coerceIn(0.4f, 2.0f)
        val t = (-0.5f * c + 0.5f) * 255f + params.brightness.coerceIn(-90f, 90f)
        val warmR = 1f + params.warmTint.coerceIn(-0.25f, 0.25f)
        val warmB = 1f - params.warmTint.coerceIn(-0.25f, 0.25f)
        val bc = ColorMatrix(
            floatArrayOf(
                c * warmR, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c * warmB, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
        combined.postConcat(bc)
        return combined
    }
}

@Composable
fun ImageEditorDialog(
    sourceUri: Uri,
    title: String = "Edit Image Before Posting",
    defaultCropAspect: ImageCropAspect = ImageCropAspect.ORIGINAL,
    onCancel: () -> Unit,
    onApply: (editedUri: Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val primary = MaterialTheme.colorScheme.primary

    var sourceBitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var rotationDeg by remember(sourceUri) { mutableIntStateOf(0) }
    var flipH by remember(sourceUri) { mutableStateOf(false) }
    var cropAspect by remember(sourceUri) { mutableStateOf(defaultCropAspect) }
    var brightness by remember(sourceUri) { mutableFloatStateOf(0f) }
    var contrast by remember(sourceUri) { mutableFloatStateOf(1f) }
    var saturation by remember(sourceUri) { mutableFloatStateOf(1f) }
    var warmTint by remember(sourceUri) { mutableFloatStateOf(0f) }
    var activeFilter by remember(sourceUri) { mutableStateOf(ImageFilterPreset.ORIGINAL) }
    var activeToolTab by remember { mutableIntStateOf(0) } // 0 = Crop & Transform, 1 = Filters, 2 = Adjust

    LaunchedEffect(sourceUri) {
        sourceBitmap = ImageEditProcessor.loadEditableBitmap(context, sourceUri, maxEdge = 960)
    }

    val params = remember(rotationDeg, flipH, cropAspect, brightness, contrast, saturation, warmTint) {
        ImageEditParams(
            rotationDeg = rotationDeg,
            flipHorizontal = flipH,
            cropAspect = cropAspect,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            warmTint = warmTint
        )
    }

    LaunchedEffect(sourceBitmap, params) {
        val src = sourceBitmap ?: return@LaunchedEffect
        val rendered = withContext(Dispatchers.Default) {
            ImageEditProcessor.applyEdits(src, params)
        }
        previewBitmap = rendered
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            color = Color(0xFF0A0D12),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onCancel,
                        enabled = !isSaving,
                        modifier = Modifier.testTag("image_editor_cancel")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Button(
                        onClick = {
                            val bmp = previewBitmap ?: sourceBitmap ?: return@Button
                            isSaving = true
                            scope.launch {
                                val res = ImageEditProcessor.saveEditedBitmapToCacheUri(context, bmp)
                                isSaving = false
                                res.onSuccess { uri -> onApply(uri) }
                            }
                        },
                        enabled = !isSaving && (previewBitmap != null || sourceBitmap != null),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("image_editor_apply")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Live Preview Stage
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF05070A))
                        .border(1.dp, Color(0xFF263244), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = previewBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Edited Image Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                        Surface(
                            color = Color(0xBB0A0D12),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "${bmp.width}×${bmp.height} • ${cropAspect.label}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = Color(0xFFD0DCEB),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        CircularProgressIndicator(color = primary)
                    }
                }

                // Tool Switcher Tabs + Controls Sheet
                Surface(
                    color = Color(0xFF131822),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = activeToolTab == 0,
                                onClick = { activeToolTab = 0 },
                                leadingIcon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Crop & Rotate") },
                                modifier = Modifier.testTag("image_editor_tab_crop")
                            )
                            FilterChip(
                                selected = activeToolTab == 1,
                                onClick = { activeToolTab = 1 },
                                label = { Text("Filters") },
                                modifier = Modifier.testTag("image_editor_tab_filters")
                            )
                            FilterChip(
                                selected = activeToolTab == 2,
                                onClick = { activeToolTab = 2 },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Adjust") },
                                modifier = Modifier.testTag("image_editor_tab_adjust")
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    rotationDeg = 0
                                    flipH = false
                                    cropAspect = defaultCropAspect
                                    brightness = 0f
                                    contrast = 1f
                                    saturation = 1f
                                    warmTint = 0f
                                    activeFilter = ImageFilterPreset.ORIGINAL
                                },
                                modifier = Modifier.testTag("image_editor_reset")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset Edits", tint = Color(0xFF9FB0C7))
                            }
                        }

                        when (activeToolTab) {
                            0 -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { rotationDeg = (rotationDeg - 90 + 360) % 360 },
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Default.RotateLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("-90°", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { rotationDeg = (rotationDeg + 90) % 360 },
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("+90°", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { flipH = !flipH },
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Flip", fontSize = 12.sp)
                                    }
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(ImageCropAspect.entries) { aspect ->
                                        FilterChip(
                                            selected = cropAspect == aspect,
                                            onClick = { cropAspect = aspect },
                                            label = { Text(aspect.label) }
                                        )
                                    }
                                }
                            }

                            1 -> {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(ImageFilterPreset.entries) { preset ->
                                        val selected = activeFilter == preset
                                        Surface(
                                            color = if (selected) primary.copy(alpha = 0.22f) else Color(0xFF1D2432),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .border(
                                                    width = if (selected) 1.5.dp else 1.dp,
                                                    color = if (selected) primary else Color(0xFF2E3A4D),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    activeFilter = preset
                                                    brightness = preset.brightness
                                                    contrast = preset.contrast
                                                    saturation = preset.saturation
                                                    warmTint = preset.warmTint
                                                }
                                        ) {
                                            Text(
                                                text = preset.label,
                                                color = if (selected) primary else Color.White,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            2 -> {
                                EditorSliderRow(
                                    label = "Brightness",
                                    valueLabel = "${brightness.roundToInt()}",
                                    value = brightness,
                                    range = -70f..70f,
                                    accent = primary,
                                    onChange = { brightness = it }
                                )
                                EditorSliderRow(
                                    label = "Contrast",
                                    valueLabel = "${(contrast * 100).roundToInt()}%",
                                    value = contrast,
                                    range = 0.5f..1.75f,
                                    accent = primary,
                                    onChange = { contrast = it }
                                )
                                EditorSliderRow(
                                    label = "Saturation",
                                    valueLabel = "${(saturation * 100).roundToInt()}%",
                                    value = saturation,
                                    range = 0f..2.0f,
                                    accent = primary,
                                    onChange = { saturation = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFFD0DCEB), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(valueLabel, color = accent, fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp)
        }
        SmoothSlider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            accentColor = accent
        )
    }
}
