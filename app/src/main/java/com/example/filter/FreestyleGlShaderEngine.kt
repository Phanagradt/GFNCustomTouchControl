package com.example.filter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.Surface
import android.view.TextureView
import com.example.model.GameFilterStyle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs

/**
 * Hardware-accelerated OpenGL ES 2.0 + GLSL Fragment Shader engine for all 17 pixel-level
 * GeForce Freestyle filter modules.
 *
 * Works identically in two modes:
 * 1. [Mode.BITMAP] — In-app live preview (60fps zero-stutter GPU preview of arena or custom image)
 * 2. [Mode.LIVE_PROJECTION] — Full-screen non-touchable overlay consuming live MediaProjection
 *    frames via SurfaceTexture (GL_TEXTURE_EXTERNAL_OES) with zero CPU pixel copies.
 */
class FreestyleGlTextureView(
    context: Context,
    private val mode: Mode
) : TextureView(context), TextureView.SurfaceTextureListener {

    enum class Mode {
        BITMAP,
        LIVE_PROJECTION
    }

    @Volatile
    private var currentStyle: GameFilterStyle? = null

    @Volatile
    private var pendingBitmap: Bitmap? = null

    @Volatile
    private var bitmapDirty: Boolean = false

    @Volatile
    private var onProjectionSurfaceReady: ((Surface?, Int, Int) -> Unit)? = null

    private var glThread: HandlerThread? = null
    private var glHandler: Handler? = null

    // EGL state (owned exclusively by glThread)
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var programId: Int = 0
    private var textureId: Int = 0
    private var inputSurfaceTexture: SurfaceTexture? = null
    private var inputSurface: Surface? = null
    private var viewportW: Int = 1
    private var viewportH: Int = 1
    private var contentW: Int = 1280
    private var contentH: Int = 720
    private var startTimeMs: Long = SystemClock.uptimeMillis()
    private val texMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    private var renderScheduled = false
    private var oldFilmAnimating = false

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(QUAD_VERTICES.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(QUAD_VERTICES)
            position(0)
        }

    // Cached uniform handles
    private var aPosition = -1
    private var aTexCoord = -1
    private var uTexMatrix = -1
    private var uTexture = -1
    private var uTexelSize = -1
    private var uTime = -1
    private var uFilterEnabled = -1
    private var uNeedsNeighbors = -1

    private var uSharpenParams = -1      // (enabled, amount, ignoreGrain, 0)
    private var uSharpenPlusParams = -1  // (enabled, intensity, textureDetails, 0)
    private var uBcParams1 = -1          // (enabled, exposure, contrast, highlights)
    private var uBcParams2 = -1          // (shadows, gamma, 0, 0)
    private var uDetailsParams = -1      // (enabled, sharpen, clarity, hdrToning)
    private var uDetailsBloom = -1       // bloom
    private var uColorParams = -1        // (enabled, temperature, vibrance, tintIntensity)
    private var uTintRgb = -1            // (r, g, b)
    private var uColorblindParams = -1   // (mode, intensity)
    private var uNightModeInt = -1       // intensity
    private var uSplitParams = -1        // (enabled, position)
    private var uBwParams = -1           // (toneIndex, intensity)
    private var uSepiaInt = -1           // intensity
    private var uVignetteParams = -1     // (enabled, intensity)
    private var uDofParams = -1          // (enabled, blurCurve, focusDepth, nearBlur)
    private var uDofFarBlur = -1         // farBlur
    private var uOldFilm1 = -1           // (enabled, gamma, exposure, contrast)
    private var uOldFilm2 = -1           // (vignette, strength, dirtStrength, 0)
    private var uMoodParams = -1         // (presetIndex, intensity)
    private var uSpecialFxParams = -1    // (modeIndex, intensity)
    private var uPainterlyParams = -1    // (enabled, edgeSharpening, filterRadius, edgeThreshold)
    private var uLetterboxParams = -1    // (enabled, height, width, 0)

    @Volatile
    private var styleDirty = true
    private var needsAnimatedTime = false

    init {
        isOpaque = (mode == Mode.LIVE_PROJECTION)
        surfaceTextureListener = this
    }

    fun setFilterStyle(style: GameFilterStyle?) {
        currentStyle = style
        styleDirty = true
        requestGlRender()
    }

    fun setSourceBitmap(bitmap: Bitmap?) {
        if (mode != Mode.BITMAP) return
        pendingBitmap = bitmap
        bitmapDirty = true
        if (bitmap != null) {
            contentW = bitmap.width.coerceAtLeast(1)
            contentH = bitmap.height.coerceAtLeast(1)
        }
        requestGlRender()
    }

    fun setProjectionSurfaceCallback(callback: ((Surface?, Int, Int) -> Unit)?) {
        onProjectionSurfaceReady = callback
        val existing = inputSurface
        if (callback != null && existing != null && existing.isValid) {
            callback(existing, viewportW, viewportH)
        }
    }

    fun updateProjectionBufferSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        contentW = width
        contentH = height
        glHandler?.post {
            runCatching { inputSurfaceTexture?.setDefaultBufferSize(width, height) }
            requestGlRenderInternal()
        }
    }

    fun requestGlRender() {
        glHandler?.post { requestGlRenderInternal() }
    }

    private fun requestGlRenderInternal() {
        if (eglSurface == EGL14.EGL_NO_SURFACE || renderScheduled) return
        renderScheduled = true
        glHandler?.post {
            renderScheduled = false
            drawFrameOnGlThread()
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        viewportW = width.coerceAtLeast(1)
        viewportH = height.coerceAtLeast(1)
        startGlThread(surface, viewportW, viewportH)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        viewportW = width.coerceAtLeast(1)
        viewportH = height.coerceAtLeast(1)
        glHandler?.post {
            if (mode == Mode.LIVE_PROJECTION) {
                val (capW, capH) = computeGpuFilterCaptureSize(viewportW, viewportH)
                contentW = capW
                contentH = capH
                runCatching { inputSurfaceTexture?.setDefaultBufferSize(capW, capH) }
                onProjectionSurfaceReady?.invoke(inputSurface, capW, capH)
            }
            drawFrameOnGlThread()
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopGlThread()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    private fun startGlThread(outputSurfaceTexture: SurfaceTexture, width: Int, height: Int) {
        stopGlThread()
        val thread = HandlerThread("FreestyleGlThread").apply { start() }
        val handler = Handler(thread.looper)
        glThread = thread
        glHandler = handler
        handler.post {
            if (initEgl(outputSurfaceTexture)) {
                initProgramAndTexture(width, height)
                drawFrameOnGlThread()
            }
        }
    }

    private fun stopGlThread() {
        val handler = glHandler
        val thread = glThread
        glHandler = null
        glThread = null
        oldFilmAnimating = false
        if (handler != null && thread != null) {
            val latch = java.util.concurrent.CountDownLatch(1)
            handler.post {
                releaseGlResources()
                latch.countDown()
            }
            runCatching { latch.await(400L, java.util.concurrent.TimeUnit.MILLISECONDS) }
            thread.quitSafely()
        }
    }

    private fun initEgl(outputSurfaceTexture: SurfaceTexture): Boolean {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false
        val versions = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, versions, 0, versions, 1)) return false

        val configAttrs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, configAttrs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] <= 0) {
            return false
        }
        val config = configs[0] ?: return false

        val ctxAttrs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, ctxAttrs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) return false

        val surfAttrs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, outputSurfaceTexture, surfAttrs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) return false

        return EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
    }

    private fun initProgramAndTexture(width: Int, height: Int) {
        val fragSrc = if (mode == Mode.LIVE_PROJECTION) {
            "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "uniform samplerExternalOES uTexture;\n" +
                FREESTYLE_FRAGMENT_BODY
        } else {
            "precision mediump float;\n" +
                "uniform sampler2D uTexture;\n" +
                FREESTYLE_FRAGMENT_BODY
        }

        programId = buildProgram(VERTEX_SHADER, fragSrc)
        if (programId == 0) return

        aPosition = GLES20.glGetAttribLocation(programId, "aPosition")
        aTexCoord = GLES20.glGetAttribLocation(programId, "aTexCoord")
        uTexMatrix = GLES20.glGetUniformLocation(programId, "uTexMatrix")
        uTexture = GLES20.glGetUniformLocation(programId, "uTexture")
        uTexelSize = GLES20.glGetUniformLocation(programId, "uTexelSize")
        uTime = GLES20.glGetUniformLocation(programId, "uTime")
        uFilterEnabled = GLES20.glGetUniformLocation(programId, "uFilterEnabled")
        uNeedsNeighbors = GLES20.glGetUniformLocation(programId, "uNeedsNeighbors")

        // Disable unused GL pipeline stages once so mobile TBDR GPUs skip depth/blend/dither work
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_DITHER)
        styleDirty = true

        uSharpenParams = GLES20.glGetUniformLocation(programId, "uSharpenParams")
        uSharpenPlusParams = GLES20.glGetUniformLocation(programId, "uSharpenPlusParams")
        uBcParams1 = GLES20.glGetUniformLocation(programId, "uBcParams1")
        uBcParams2 = GLES20.glGetUniformLocation(programId, "uBcParams2")
        uDetailsParams = GLES20.glGetUniformLocation(programId, "uDetailsParams")
        uDetailsBloom = GLES20.glGetUniformLocation(programId, "uDetailsBloom")
        uColorParams = GLES20.glGetUniformLocation(programId, "uColorParams")
        uTintRgb = GLES20.glGetUniformLocation(programId, "uTintRgb")
        uColorblindParams = GLES20.glGetUniformLocation(programId, "uColorblindParams")
        uNightModeInt = GLES20.glGetUniformLocation(programId, "uNightModeInt")
        uSplitParams = GLES20.glGetUniformLocation(programId, "uSplitParams")
        uBwParams = GLES20.glGetUniformLocation(programId, "uBwParams")
        uSepiaInt = GLES20.glGetUniformLocation(programId, "uSepiaInt")
        uVignetteParams = GLES20.glGetUniformLocation(programId, "uVignetteParams")
        uDofParams = GLES20.glGetUniformLocation(programId, "uDofParams")
        uDofFarBlur = GLES20.glGetUniformLocation(programId, "uDofFarBlur")
        uOldFilm1 = GLES20.glGetUniformLocation(programId, "uOldFilm1")
        uOldFilm2 = GLES20.glGetUniformLocation(programId, "uOldFilm2")
        uMoodParams = GLES20.glGetUniformLocation(programId, "uMoodParams")
        uSpecialFxParams = GLES20.glGetUniformLocation(programId, "uSpecialFxParams")
        uPainterlyParams = GLES20.glGetUniformLocation(programId, "uPainterlyParams")
        uLetterboxParams = GLES20.glGetUniformLocation(programId, "uLetterboxParams")

        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        textureId = texIds[0]
        val target = if (mode == Mode.LIVE_PROJECTION) {
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        } else {
            GLES20.GL_TEXTURE_2D
        }
        GLES20.glBindTexture(target, textureId)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        if (mode == Mode.LIVE_PROJECTION) {
            val (capW, capH) = computeGpuFilterCaptureSize(width, height)
            contentW = capW
            contentH = capH
            val st = SurfaceTexture(textureId).apply {
                setDefaultBufferSize(capW, capH)
                setOnFrameAvailableListener({
                    requestGlRenderInternal()
                }, glHandler)
            }
            inputSurfaceTexture = st
            val surf = Surface(st)
            inputSurface = surf
            onProjectionSurfaceReady?.invoke(surf, capW, capH)
        } else {
            bitmapDirty = true
        }
    }

    private fun drawFrameOnGlThread() {
        if (eglSurface == EGL14.EGL_NO_SURFACE || programId == 0) return
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return

        if (mode == Mode.LIVE_PROJECTION) {
            val st = inputSurfaceTexture ?: return
            runCatching {
                st.updateTexImage()
                st.getTransformMatrix(texMatrix)
            }
        } else {
            Matrix.setIdentityM(texMatrix, 0)
            if (bitmapDirty) {
                val bmp = pendingBitmap
                if (bmp != null && !bmp.isRecycled) {
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
                    contentW = bmp.width.coerceAtLeast(1)
                    contentH = bmp.height.coerceAtLeast(1)
                    bitmapDirty = false
                }
            }
        }

        GLES20.glViewport(0, 0, viewportW, viewportH)
        GLES20.glUseProgram(programId)

        val target = if (mode == Mode.LIVE_PROJECTION) {
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        } else {
            GLES20.GL_TEXTURE_2D
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(target, textureId)
        GLES20.glUniform1i(uTexture, 0)
        GLES20.glUniformMatrix4fv(uTexMatrix, 1, false, texMatrix, 0)
        GLES20.glUniform2f(
            uTexelSize,
            1f / contentW.coerceAtLeast(64).toFloat(),
            1f / contentH.coerceAtLeast(64).toFloat()
        )
        if (styleDirty) {
            styleDirty = false
            uploadStyleUniforms(currentStyle)
        }
        if (needsAnimatedTime) {
            val elapsedSec = ((SystemClock.uptimeMillis() - startTimeMs) % 600_000L) / 1000f
            GLES20.glUniform1f(uTime, elapsedSec)
        }

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)

        vertexBuffer.position(2)
        GLES20.glEnableVertexAttribArray(aTexCoord)
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTexCoord)

        EGL14.eglSwapBuffers(eglDisplay, eglSurface)

        // Animate procedural film grain/flicker/scratches in Bitmap preview mode
        val s = currentStyle
        val needsAnim = mode == Mode.BITMAP && s != null && s.oldFilmEnabled && s.oldFilmDirtStrength > 0.02f
        if (needsAnim && !oldFilmAnimating) {
            oldFilmAnimating = true
            glHandler?.postDelayed({
                oldFilmAnimating = false
                requestGlRenderInternal()
            }, 42L)
        }
    }

    private fun uploadStyleUniforms(style: GameFilterStyle?) {
        if (style == null) {
            GLES20.glUniform1f(uFilterEnabled, 0f)
            GLES20.glUniform1f(uNeedsNeighbors, 0f)
            needsAnimatedTime = false
            return
        }
        GLES20.glUniform1f(uFilterEnabled, 1f)
        val wantsNeighbors = styleRequiresNeighborTaps(style)
        GLES20.glUniform1f(uNeedsNeighbors, if (wantsNeighbors) 1f else 0f)
        needsAnimatedTime = style.oldFilmEnabled && style.oldFilmDirtStrength > 0.005f

        GLES20.glUniform4f(
            uSharpenParams,
            if (style.sharpenEnabled) 1f else 0f,
            style.sharpenAmount.coerceIn(0f, 1f),
            style.sharpenIgnoreFilmGrain.coerceIn(0f, 1f),
            0f
        )
        GLES20.glUniform4f(
            uSharpenPlusParams,
            if (style.sharpenPlusEnabled) 1f else 0f,
            style.sharpenPlusIntensity.coerceIn(0f, 1f),
            style.sharpenPlusTextureDetails.coerceIn(-1f, 1f),
            0f
        )
        GLES20.glUniform4f(
            uBcParams1,
            if (style.brightnessContrastEnabled) 1f else 0f,
            style.effectiveExposure.coerceIn(-1f, 1f),
            style.contrast.coerceIn(-1f, 1f),
            style.highlights.coerceIn(-1f, 1f)
        )
        GLES20.glUniform4f(
            uBcParams2,
            style.shadows.coerceIn(-1f, 1f),
            style.gamma.coerceIn(-1f, 1f),
            0f,
            0f
        )
        GLES20.glUniform4f(
            uDetailsParams,
            if (style.detailsEnabled) 1f else 0f,
            style.detailsSharpen.coerceIn(0f, 1f),
            style.detailsClarity.coerceIn(-1f, 1f),
            style.detailsHdrToning.coerceIn(-1f, 1f)
        )
        GLES20.glUniform1f(
            uDetailsBloom,
            if (style.detailsEnabled) style.detailsBloom.coerceIn(0f, 1f) else 0f
        )
        val (tr, tg, tb) = style.resolvedTintRgb()
        GLES20.glUniform4f(
            uColorParams,
            if (style.colorEnabled) 1f else 0f,
            style.temperature.coerceIn(-1f, 1f),
            style.colorVibrance.coerceIn(-1f, 1f),
            style.tintIntensity.coerceIn(0f, 1f)
        )
        GLES20.glUniform3f(uTintRgb, tr, tg, tb)

        GLES20.glUniform2f(
            uColorblindParams,
            if (style.colorblindEnabled) style.colorblindMode.shaderIndex.toFloat() else 0f,
            style.colorblindIntensity.coerceIn(0f, 1f)
        )
        GLES20.glUniform1f(
            uNightModeInt,
            if (style.nightModeEnabled) style.nightModeIntensity.coerceIn(0f, 1f) else 0f
        )
        GLES20.glUniform2f(
            uSplitParams,
            if (style.splitscreenEnabled) 1f else 0f,
            style.splitscreenPosition.coerceIn(0f, 1f)
        )
        GLES20.glUniform2f(
            uBwParams,
            style.blackWhiteTone.shaderIndex.toFloat(),
            if (style.blackWhiteEnabled) style.blackWhiteIntensity.coerceIn(0f, 1f) else 0f
        )
        GLES20.glUniform1f(
            uSepiaInt,
            if (style.sepiaEnabled) style.sepiaIntensity.coerceIn(0f, 1f) else 0f
        )
        GLES20.glUniform2f(
            uVignetteParams,
            if (style.vignetteEnabled) 1f else 0f,
            style.vignetteIntensity.coerceIn(-1f, 1f)
        )
        GLES20.glUniform4f(
            uDofParams,
            if (style.dofEnabled) 1f else 0f,
            style.dofBlurCurve.coerceIn(0.1f, 1f),
            style.dofFocusDepth.coerceIn(0f, 1f),
            style.dofNearBlur.coerceIn(0f, 1f)
        )
        GLES20.glUniform1f(
            uDofFarBlur,
            if (style.dofEnabled) style.dofFarBlur.coerceIn(0f, 1f) else 0f
        )
        GLES20.glUniform4f(
            uOldFilm1,
            if (style.oldFilmEnabled) 1f else 0f,
            style.oldFilmGamma.coerceIn(-1f, 1f),
            style.oldFilmExposure.coerceIn(-1f, 1f),
            style.oldFilmContrast.coerceIn(-1f, 1f)
        )
        GLES20.glUniform4f(
            uOldFilm2,
            style.oldFilmVignette.coerceIn(0f, 1f),
            style.oldFilmStrength.coerceIn(0f, 1f),
            style.oldFilmDirtStrength.coerceIn(0f, 1f),
            0f
        )
        GLES20.glUniform2f(
            uMoodParams,
            if (style.moodEnabled) style.moodPreset.shaderIndex.toFloat() else 0f,
            style.moodIntensity.coerceIn(0f, 1f)
        )
        GLES20.glUniform2f(
            uSpecialFxParams,
            if (style.specialFxEnabled) style.specialFxMode.shaderIndex.toFloat() else 0f,
            style.specialFxIntensity.coerceIn(0f, 1f)
        )
        GLES20.glUniform4f(
            uPainterlyParams,
            if (style.painterlyEnabled) 1f else 0f,
            style.painterlyEdgeSharpening.coerceIn(0f, 1f),
            style.painterlyFilterRadius.coerceIn(0.1f, 1f),
            style.painterlyEdgeThreshold.coerceIn(0.05f, 1f)
        )
        GLES20.glUniform4f(
            uLetterboxParams,
            if (style.letterboxEnabled) 1f else 0f,
            style.letterboxHeight.coerceIn(0f, 0.42f),
            style.letterboxWidth.coerceIn(0f, 0.42f),
            0f
        )
    }

    private fun releaseGlResources() {
        onProjectionSurfaceReady?.invoke(null, 0, 0)
        runCatching { inputSurface?.release() }
        inputSurface = null
        runCatching { inputSurfaceTexture?.release() }
        inputSurfaceTexture = null

        if (textureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
        }
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                eglSurface = EGL14.EGL_NO_SURFACE
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                eglContext = EGL14.EGL_NO_CONTEXT
            }
            EGL14.eglTerminate(eglDisplay)
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }
    }

    private fun buildProgram(vertexSrc: String, fragmentSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        if (vs == 0) return 0
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
        if (fs == 0) {
            GLES20.glDeleteShader(vs)
            return 0
        }
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(program)
            return 0
        }
        return program
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    companion object {
        const val GPU_FILTER_MAX_LONG_EDGE = 1280

        /**
         * Scales the live MediaProjection GPU filter VirtualDisplay down to a crisp 720p-class
         * capture buffer (preserving exact aspect ratio and even dimensions). Cuts pixel fillrate
         * by 3x-5x on 1080p/1440p displays while GL_LINEAR hardware upscaling keeps gameplay sharp.
         */
        fun computeGpuFilterCaptureSize(
            displayWidth: Int,
            displayHeight: Int,
            maxLongEdge: Int = GPU_FILTER_MAX_LONG_EDGE
        ): Pair<Int, Int> {
            val w = displayWidth.coerceAtLeast(320)
            val h = displayHeight.coerceAtLeast(320)
            val longEdge = maxOf(w, h)
            if (longEdge <= maxLongEdge) {
                return (w and 1.inv()).coerceAtLeast(320) to (h and 1.inv()).coerceAtLeast(320)
            }
            val scale = maxLongEdge.toFloat() / longEdge.toFloat()
            val outW = ((w * scale).toInt() and 1.inv()).coerceAtLeast(320)
            val outH = ((h * scale).toInt() and 1.inv()).coerceAtLeast(320)
            return outW to outH
        }

        /**
         * Returns true only when an active filter module actually reads 4-way neighbor texels.
         * Keeping this false for pure color/tone grading cuts OES YUV texture fetches from 6 -> 1.
         */
        fun styleRequiresNeighborTaps(style: GameFilterStyle): Boolean {
            if (style.sharpenEnabled && style.sharpenAmount > 0.005f) return true
            if (style.sharpenPlusEnabled && (style.sharpenPlusIntensity > 0.005f || abs(style.sharpenPlusTextureDetails) > 0.005f)) return true
            if (style.detailsEnabled && style.detailsSharpen > 0.005f) return true
            if (style.painterlyEnabled) return true
            if (style.specialFxEnabled && style.specialFxMode == com.example.model.SpecialFxMode.SKETCH && style.specialFxIntensity > 0.005f) return true
            return false
        }

        // Interleaved X, Y, U, V (top-left origin for Android Bitmap & SurfaceTexture)
        private val QUAD_VERTICES = floatArrayOf(
            -1f, -1f, 0f, 1f,
            1f, -1f, 1f, 1f,
            -1f, 1f, 0f, 0f,
            1f, 1f, 1f, 0f
        )

        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            uniform mat4 uTexMatrix;
            varying vec2 vTexCoord;
            varying vec2 vScreenUv;
            void main() {
                gl_Position = aPosition;
                vScreenUv = aTexCoord;
                vec4 transformed = uTexMatrix * vec4(aTexCoord.x, 1.0 - aTexCoord.y, 0.0, 1.0);
                vTexCoord = vec2(transformed.x, 1.0 - transformed.y);
            }
        """

        /**
         * Complete 17-module GeForce Freestyle GLSL ES 1.00 fragment shader body.
         * Expects `uTexture` (either sampler2D or samplerExternalOES) declared above it.
         */
        const val FREESTYLE_FRAGMENT_BODY = """
            varying vec2 vTexCoord;
            varying vec2 vScreenUv;
            uniform vec2 uTexelSize;
            uniform float uTime;
            uniform float uFilterEnabled;
            uniform float uNeedsNeighbors;

            uniform vec4 uSharpenParams;      // (enabled, amount, ignoreGrain, 0)
            uniform vec4 uSharpenPlusParams;  // (enabled, intensity, textureDetails, 0)
            uniform vec4 uBcParams1;          // (enabled, exposure, contrast, highlights)
            uniform vec4 uBcParams2;          // (shadows, gamma, 0, 0)
            uniform vec4 uDetailsParams;      // (enabled, sharpen, clarity, hdrToning)
            uniform float uDetailsBloom;
            uniform vec4 uColorParams;        // (enabled, temperature, vibrance, tintIntensity)
            uniform vec3 uTintRgb;
            uniform vec2 uColorblindParams;   // (mode: 0..3, intensity)
            uniform float uNightModeInt;
            uniform vec2 uSplitParams;        // (enabled, position)
            uniform vec2 uBwParams;           // (tone: 0..3, intensity)
            uniform float uSepiaInt;
            uniform vec2 uVignetteParams;     // (enabled, intensity)
            uniform vec4 uDofParams;          // (enabled, blurCurve, focusDepth, nearBlur)
            uniform float uDofFarBlur;
            uniform vec4 uOldFilm1;           // (enabled, gamma, exposure, contrast)
            uniform vec4 uOldFilm2;           // (vignette, strength, dirtStrength, 0)
            uniform vec2 uMoodParams;         // (preset: 0..6, intensity)
            uniform vec2 uSpecialFxParams;    // (mode: 0..3, intensity)
            uniform vec4 uPainterlyParams;    // (enabled, edgeSharp, radius, edgeThreshold)
            uniform vec4 uLetterboxParams;    // (enabled, height, width, 0)

            float luma(vec3 c) {
                return dot(c, vec3(0.2126, 0.7152, 0.0722));
            }

            float randHash(vec2 p) {
                return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
            }

            vec3 sampleTex(vec2 uv) {
                return texture2D(uTexture, clamp(uv, vec2(0.001), vec2(0.999))).rgb;
            }

            vec3 applyDaltonize(vec3 rgb, float mode, float intensity) {
                float L = 17.8824 * rgb.r + 43.5161 * rgb.g + 4.11935 * rgb.b;
                float M = 3.45565 * rgb.r + 27.1554 * rgb.g + 3.86714 * rgb.b;
                float S = 0.0299566 * rgb.r + 0.184309 * rgb.g + 1.46709 * rgb.b;
                float Ls = L;
                float Ms = M;
                float Ss = S;
                if (mode < 1.5) {
                    Ls = 2.02344 * M - 2.52581 * S;
                } else if (mode < 2.5) {
                    Ms = 0.494207 * L + 1.24827 * S;
                } else {
                    Ss = -0.395913 * L + 0.801109 * M;
                }
                vec3 sim = vec3(
                    0.080944 * Ls - 0.130504 * Ms + 0.116721 * Ss,
                    -0.0102485 * Ls + 0.0540194 * Ms - 0.113615 * Ss,
                    -0.000365294 * Ls - 0.00412163 * Ms + 0.693513 * Ss
                );
                vec3 err = rgb - clamp(sim, 0.0, 1.0);
                vec3 shift = vec3(
                    0.7 * err.g + 0.7 * err.b,
                    0.7 * err.r,
                    0.7 * err.r + 0.7 * err.g
                );
                return clamp(rgb + shift * intensity, 0.0, 1.0);
            }

            vec3 applyMood(vec3 c, float preset, float intensity) {
                vec3 graded = c;
                float y = luma(c);
                if (preset < 1.5) {
                    // Warm Sunset: golden highlights, warm magenta-amber shadows
                    vec3 sh = vec3(0.22, 0.08, 0.16);
                    vec3 hi = vec3(1.12, 0.96, 0.78);
                    graded = mix(c * 0.85 + sh * 0.35, c * hi, smoothstep(0.15, 0.85, y));
                } else if (preset < 2.5) {
                    // Cold Thriller: teal shadows, crisp steel highlights
                    vec3 sh = vec3(0.02, 0.12, 0.20);
                    vec3 hi = vec3(0.92, 1.02, 1.12);
                    graded = mix(c * 0.88 + sh * 0.40, c * hi, smoothstep(0.15, 0.85, y));
                } else if (preset < 3.5) {
                    // Washed Retro: lifted matte blacks, faded highlights, warm cast
                    graded = c * 0.84 + vec3(0.10, 0.09, 0.07);
                    graded = mix(vec3(luma(graded)), graded, 0.80);
                } else if (preset < 4.5) {
                    // Cyber Neon: magenta shadows, electric cyan highlights
                    vec3 sh = vec3(0.24, 0.02, 0.28);
                    vec3 hi = vec3(0.86, 1.10, 1.18);
                    graded = mix(c * 0.90 + sh * 0.32, c * hi, smoothstep(0.20, 0.80, y));
                } else if (preset < 5.5) {
                    // Emerald Cinema: rich green-teal shadows, subtle warm skin highlights
                    graded = vec3(c.r * 0.92, c.g * 1.08 + 0.03 * (1.0 - y), c.b * 0.98 + 0.04 * (1.0 - y));
                } else {
                    // Golden Noir: deep contrast with gold-tinted highlights
                    vec3 mono = vec3(y);
                    vec3 gold = vec3(y * 1.15, y * 1.02, y * 0.72);
                    graded = mix(mono * 0.82, gold, smoothstep(0.35, 0.90, y));
                    graded = mix(graded, c, 0.28);
                }
                return mix(c, clamp(graded, 0.0, 1.0), intensity);
            }

            void main() {
                vec2 uv = vTexCoord;
                vec3 origColor = sampleTex(uv);

                if (uFilterEnabled < 0.5) {
                    gl_FragColor = vec4(origColor, 1.0);
                    return;
                }

                // Splitscreen left-side bypass + crisp vertical comparison bar
                if (uSplitParams.x > 0.5) {
                    float distToSplit = abs(vScreenUv.x - uSplitParams.y);
                    if (distToSplit < 0.0028) {
                        gl_FragColor = vec4(0.92, 1.0, 0.75, 1.0);
                        return;
                    }
                    if (vScreenUv.x < uSplitParams.y) {
                        gl_FragColor = vec4(origColor, 1.0);
                        return;
                    }
                }

                // Special FX: Retro Pixelation UV snap (reuse origColor when off to save a full-screen OES fetch!)
                vec2 workUv = uv;
                vec3 col = origColor;
                if (uSpecialFxParams.x > 0.5 && uSpecialFxParams.x < 1.5 && uSpecialFxParams.y > 0.01) {
                    float pixSize = mix(1.0, 9.0, uSpecialFxParams.y);
                    vec2 stepUv = uTexelSize * pixSize;
                    workUv = (floor(uv / stepUv) + 0.5) * stepUv;
                    col = sampleTex(workUv);
                }

                // Conditional 4-way neighbor taps: skipped completely for pure Color/Brightness/Mood/Night filters!
                vec2 tx = uTexelSize;
                vec3 nL = col;
                vec3 nR = col;
                vec3 nU = col;
                vec3 nD = col;
                if (uNeedsNeighbors > 0.5) {
                    nL = sampleTex(workUv + vec2(-tx.x, 0.0));
                    nR = sampleTex(workUv + vec2( tx.x, 0.0));
                    nU = sampleTex(workUv + vec2(0.0, -tx.y));
                    nD = sampleTex(workUv + vec2(0.0,  tx.y));
                }

                // 3H. Painterly (Kuwahara-style smoothing + cel-shaded edge outline)
                if (uPainterlyParams.x > 0.5) {
                    float rad = mix(1.2, 4.2, uPainterlyParams.z);
                    vec2 rOff = tx * rad;
                    vec3 q1 = 0.25 * (col + sampleTex(workUv + vec2(-rOff.x, 0.0)) + sampleTex(workUv + vec2(0.0, -rOff.y)) + sampleTex(workUv + vec2(-rOff.x, -rOff.y)));
                    vec3 q2 = 0.25 * (col + sampleTex(workUv + vec2( rOff.x, 0.0)) + sampleTex(workUv + vec2(0.0, -rOff.y)) + sampleTex(workUv + vec2( rOff.x, -rOff.y)));
                    vec3 q3 = 0.25 * (col + sampleTex(workUv + vec2(-rOff.x, 0.0)) + sampleTex(workUv + vec2(0.0,  rOff.y)) + sampleTex(workUv + vec2(-rOff.x,  rOff.y)));
                    vec3 q4 = 0.25 * (col + sampleTex(workUv + vec2( rOff.x, 0.0)) + sampleTex(workUv + vec2(0.0,  rOff.y)) + sampleTex(workUv + vec2( rOff.x,  rOff.y)));
                    float d1 = abs(luma(q1) - luma(col));
                    float d2 = abs(luma(q2) - luma(col));
                    float d3 = abs(luma(q3) - luma(col));
                    float d4 = abs(luma(q4) - luma(col));
                    vec3 bestQ = q1;
                    float bestD = d1;
                    if (d2 < bestD) { bestD = d2; bestQ = q2; }
                    if (d3 < bestD) { bestD = d3; bestQ = q3; }
                    if (d4 < bestD) { bestQ = q4; }
                    // Subtle paint palette posterization
                    vec3 painted = floor(bestQ * 14.0 + 0.5) / 14.0;
                    col = mix(bestQ, painted, 0.45);
                    // Edge detection ink stroke
                    float gx = luma(nR) - luma(nL);
                    float gy = luma(nD) - luma(nU);
                    float edge = sqrt(gx * gx + gy * gy);
                    float edgeMask = smoothstep(uPainterlyParams.w * 0.45, uPainterlyParams.w * 0.45 + 0.14, edge);
                    col = mix(col, col * (1.0 - 0.85 * uPainterlyParams.y), edgeMask);
                }

                // 3D. Depth of Field / Tilt-Shift (real multi-tap GPU bokeh)
                if (uDofParams.x > 0.5) {
                    float distY = vScreenUv.y - uDofParams.z;
                    float bandHalf = mix(0.04, 0.26, 1.0 - uDofParams.y);
                    float blurFactor = smoothstep(bandHalf, bandHalf + 0.34, abs(distY));
                    float strength = (distY > 0.0) ? uDofParams.w : uDofFarBlur;
                    float blurPx = blurFactor * strength * 7.5;
                    if (blurPx > 0.25) {
                        vec2 b1 = tx * blurPx;
                        vec2 b2 = tx * blurPx * 0.65;
                        vec3 acc = col * 0.16;
                        acc += sampleTex(workUv + vec2( b1.x, 0.0)) * 0.11;
                        acc += sampleTex(workUv + vec2(-b1.x, 0.0)) * 0.11;
                        acc += sampleTex(workUv + vec2(0.0,  b1.y)) * 0.11;
                        acc += sampleTex(workUv + vec2(0.0, -b1.y)) * 0.11;
                        acc += sampleTex(workUv + vec2( b2.x,  b2.y)) * 0.10;
                        acc += sampleTex(workUv + vec2(-b2.x,  b2.y)) * 0.10;
                        acc += sampleTex(workUv + vec2( b2.x, -b2.y)) * 0.10;
                        acc += sampleTex(workUv + vec2(-b2.x, -b2.y)) * 0.10;
                        col = acc;
                    }
                }

                // 1A. Sharpen + Ignore Film Grain
                if (uSharpenParams.x > 0.5 && uSharpenParams.y > 0.005) {
                    vec3 blur4 = 0.25 * (nL + nR + nU + nD);
                    vec3 highPass = col - blur4;
                    float edgeMag = abs(luma(highPass));
                    float grainThresh = uSharpenParams.z * 0.095;
                    float grainGate = smoothstep(grainThresh, grainThresh + 0.04, edgeMag);
                    col = clamp(col + highPass * (uSharpenParams.y * 2.2) * grainGate, 0.0, 1.0);
                }

                // 1B. Sharpen+ (Texture-Aware: enhances fine textures without over-sharpening UI text)
                if (uSharpenPlusParams.x > 0.5 && (uSharpenPlusParams.y > 0.005 || abs(uSharpenPlusParams.z) > 0.005)) {
                    vec3 nUL = sampleTex(workUv + vec2(-tx.x, -tx.y));
                    vec3 nUR = sampleTex(workUv + vec2( tx.x, -tx.y));
                    vec3 nDL = sampleTex(workUv + vec2(-tx.x,  tx.y));
                    vec3 nDR = sampleTex(workUv + vec2( tx.x,  tx.y));
                    vec3 blur8 = (nL + nR + nU + nD) * 0.15 + (nUL + nUR + nDL + nDR) * 0.10;
                    vec3 detail = col - blur8;
                    float diffL = abs(luma(detail));
                    // Clamp extreme high-contrast UI text edges while boosting fine textures
                    float uiTextGuard = 1.0 - smoothstep(0.18, 0.42, diffL);
                    float fineGain = uSharpenPlusParams.y * 1.9 + uSharpenPlusParams.z * 1.6 * uiTextGuard;
                    col = clamp(col + clamp(detail * fineGain, vec3(-0.28), vec3(0.28)), 0.0, 1.0);
                }

                // 1D. Details (Sharpen, Clarity, HDR Toning, Bloom)
                if (uDetailsParams.x > 0.5) {
                    vec2 wide = tx * 3.0;
                    vec3 wL = sampleTex(workUv + vec2(-wide.x, 0.0));
                    vec3 wR = sampleTex(workUv + vec2( wide.x, 0.0));
                    vec3 wU = sampleTex(workUv + vec2(0.0, -wide.y));
                    vec3 wD = sampleTex(workUv + vec2(0.0,  wide.y));
                    vec3 wideBlur = 0.25 * (wL + wR + wU + wD);

                    // General edge sharpen
                    if (uDetailsParams.y > 0.005) {
                        vec3 blur4 = 0.25 * (nL + nR + nU + nD);
                        col = clamp(col + (col - blur4) * (uDetailsParams.y * 1.6), 0.0, 1.0);
                    }
                    // Clarity: local mid-tone contrast
                    if (abs(uDetailsParams.z) > 0.005) {
                        float y = luma(col);
                        float midMask = 1.0 - abs(y * 2.0 - 1.0);
                        col = clamp(col + (col - wideBlur) * (uDetailsParams.z * 1.35) * (0.35 + 0.65 * midMask), 0.0, 1.0);
                    }
                    // HDR Toning: simultaneous shadow lift + highlight balance + local pop
                    if (abs(uDetailsParams.w) > 0.005) {
                        float hAmt = uDetailsParams.w;
                        float y = luma(col);
                        float darkLift = pow(1.0 - y, 2.2) * hAmt * 0.34;
                        float brightComp = pow(y, 2.4) * hAmt * 0.16;
                        col = clamp(col + vec3(darkLift - brightComp) + (col - wideBlur) * (hAmt * 0.45), 0.0, 1.0);
                    }
                    // Bloom: bright highlight bleed
                    if (uDetailsBloom > 0.005) {
                        vec3 bright = max(wideBlur - vec3(0.62), vec3(0.0));
                        col = clamp(col + bright * (uDetailsBloom * 1.65), 0.0, 1.0);
                    }
                }

                // 1C. Brightness / Contrast (Exposure, Contrast, Highlights, Shadows, Gamma)
                if (uBcParams1.x > 0.5) {
                    // Exposure
                    float expScale = pow(2.0, uBcParams1.y * 1.35);
                    col = clamp(col * expScale, 0.0, 1.0);

                    // Highlights (bright-zone recovery or boost)
                    float y1 = luma(col);
                    float hiMask = smoothstep(0.48, 0.98, y1);
                    col = clamp(col + col * (uBcParams1.w * 0.48) * hiMask, 0.0, 1.0);

                    // Shadows (brightens dark corners without washing out highlights)
                    float shMask = pow(1.0 - smoothstep(0.0, 0.62, y1), 1.6);
                    if (uBcParams2.x >= 0.0) {
                        col = clamp(col + (1.0 - col) * (uBcParams2.x * 0.44) * shMask, 0.0, 1.0);
                    } else {
                        col = clamp(col * (1.0 + uBcParams2.x * 0.55 * shMask), 0.0, 1.0);
                    }

                    // Contrast
                    float cFactor = 1.0 + uBcParams1.z * 0.85;
                    col = clamp((col - 0.5) * cFactor + 0.5, 0.0, 1.0);

                    // Gamma (mid-tone brightness curve)
                    float gExp = pow(2.0, -uBcParams2.y * 0.85);
                    col = pow(max(col, vec3(0.0001)), vec3(gExp));
                }

                // 1E. Color (Temperature, Vibrance, Tint Hue & Intensity)
                if (uColorParams.x > 0.5) {
                    // Temperature (warm amber vs cool blue)
                    float temp = uColorParams.y;
                    col.r = clamp(col.r * (1.0 + temp * 0.24), 0.0, 1.0);
                    col.g = clamp(col.g * (1.0 + temp * 0.04), 0.0, 1.0);
                    col.b = clamp(col.b * (1.0 - temp * 0.28), 0.0, 1.0);

                    // True Vibrance (boosts muted colors more than already saturated colors)
                    float vib = uColorParams.z;
                    float maxC = max(col.r, max(col.g, col.b));
                    float minC = min(col.r, min(col.g, col.b));
                    float curSat = maxC - minC;
                    float y = luma(col);
                    float vibFactor = 1.0 + vib * (1.0 - curSat * 0.72);
                    col = clamp(mix(vec3(y), col, max(vibFactor, 0.0)), 0.0, 1.0);

                    // Tint Color & Intensity (luminance-preserving colorization + soft overlay)
                    if (uColorParams.w > 0.005) {
                        float yT = luma(col);
                        vec3 tinted = mix(col * (0.45 + uTintRgb * 0.85), vec3(yT) * (uTintRgb * 1.15 + 0.15), 0.45);
                        col = clamp(mix(col, tinted, uColorParams.w * 0.78), 0.0, 1.0);
                    }
                }

                // 2A. Colorblind Daltonization (Protanopia / Deuteranopia / Tritanopia)
                if (uColorblindParams.x > 0.5 && uColorblindParams.y > 0.005) {
                    col = applyDaltonize(col, uColorblindParams.x, uColorblindParams.y);
                }

                // 2B. Night Mode (Blue-light spectral attenuation)
                if (uNightModeInt > 0.005) {
                    vec3 warmNight = vec3(
                        min(col.r * (1.0 + uNightModeInt * 0.08), 1.0),
                        col.g * (1.0 - uNightModeInt * 0.14),
                        col.b * (1.0 - uNightModeInt * 0.62)
                    );
                    col = warmNight;
                }

                // 3F. Mood Cinematic Color-Grading LUTs
                if (uMoodParams.x > 0.5 && uMoodParams.y > 0.005) {
                    col = applyMood(col, uMoodParams.x, uMoodParams.y);
                }

                // 3A. Black & White (Mono / Warm / Cool / High Contrast)
                if (uBwParams.y > 0.005) {
                    float y = luma(col);
                    vec3 bwCol = vec3(y);
                    if (uBwParams.x > 0.5 && uBwParams.x < 1.5) {
                        bwCol = vec3(y * 1.08, y * 1.01, y * 0.88);
                    } else if (uBwParams.x >= 1.5 && uBwParams.x < 2.5) {
                        bwCol = vec3(y * 0.90, y * 0.99, y * 1.12);
                    } else if (uBwParams.x >= 2.5) {
                        float hc = clamp((y - 0.5) * 1.48 + 0.5, 0.0, 1.0);
                        bwCol = vec3(hc);
                    }
                    col = mix(col, clamp(bwCol, 0.0, 1.0), uBwParams.y);
                }

                // 3B. Sepia
                if (uSepiaInt > 0.005) {
                    vec3 sep = vec3(
                        dot(col, vec3(0.393, 0.769, 0.189)),
                        dot(col, vec3(0.349, 0.686, 0.168)),
                        dot(col, vec3(0.272, 0.534, 0.131))
                    );
                    col = mix(col, clamp(sep, 0.0, 1.0), uSepiaInt);
                }

                // 3E. Old Film (Gamma, Exposure, Contrast, Vignette, Strength, Dirt/Grain/Scratches)
                if (uOldFilm1.x > 0.5 && uOldFilm2.y > 0.005) {
                    vec3 film = col;
                    // Exposure & Contrast & Gamma
                    film = clamp(film * pow(2.0, uOldFilm1.z * 1.2), 0.0, 1.0);
                    film = clamp((film - 0.5) * (1.0 + uOldFilm1.w * 0.9) + 0.5, 0.0, 1.0);
                    film = pow(max(film, vec3(0.0001)), vec3(pow(2.0, -uOldFilm1.y * 0.8)));
                    // Vintage faded cinema tone
                    float fy = luma(film);
                    vec3 vintage = mix(vec3(fy * 1.06, fy * 0.98, fy * 0.82), film, 0.35);
                    vintage = vintage * 0.90 + vec3(0.06, 0.05, 0.03);
                    // Animated flicker + grain + vertical film scratches + dust dirt
                    if (uOldFilm2.z > 0.005) {
                        float dirt = uOldFilm2.z;
                        float flicker = 1.0 + (randHash(vec2(floor(uTime * 14.0), 1.7)) - 0.5) * 0.14 * dirt;
                        float grain = (randHash(vScreenUv * 520.0 + vec2(uTime * 37.0, uTime * 19.0)) - 0.5) * 0.24 * dirt;
                        // Vertical scratch line drifting horizontally
                        float scratchX = fract(randHash(vec2(floor(uTime * 4.5), 9.1)));
                        float scratch = smoothstep(0.0032, 0.0, abs(vScreenUv.x - scratchX)) * 0.42 * dirt;
                        // Occasional dust specks
                        float dustCell = randHash(floor(vScreenUv * 26.0) + vec2(floor(uTime * 9.0), 3.1));
                        float dust = step(0.996 - 0.006 * dirt, dustCell) * 0.45 * dirt;
                        vintage = clamp(vintage * flicker + vec3(grain + scratch - dust), 0.0, 1.0);
                    }
                    // Old film vignette
                    float fd = distance(vScreenUv, vec2(0.5));
                    float fVig = smoothstep(0.32, 0.84, fd) * uOldFilm2.x * 0.82;
                    vintage *= (1.0 - fVig);
                    col = mix(col, clamp(vintage, 0.0, 1.0), uOldFilm2.y);
                }

                // 3G. Special FX (Retro Palette Quantization / Pencil Sketch / Half-Tone Comic)
                if (uSpecialFxParams.x > 0.5 && uSpecialFxParams.y > 0.005) {
                    float fxInt = uSpecialFxParams.y;
                    if (uSpecialFxParams.x < 1.5) {
                        // Retro 16-bit palette quantization + subtle CRT scanline
                        float levels = mix(24.0, 6.0, fxInt);
                        vec3 quant = floor(col * levels + 0.5) / levels;
                        float scan = 1.0 - (sin(vScreenUv.y * 420.0) * 0.5 + 0.5) * 0.16 * fxInt;
                        col = mix(col, quant * scan, fxInt);
                    } else if (uSpecialFxParams.x < 2.5) {
                        // Pencil Sketch (Sobel edge ink on warm paper)
                        float gx = luma(nR) - luma(nL);
                        float gy = luma(nD) - luma(nU);
                        float edge = clamp(sqrt(gx * gx + gy * gy) * 5.2, 0.0, 1.0);
                        float hatch = (randHash(floor(vScreenUv * vec2(260.0, 260.0))) - 0.5) * 0.08;
                        vec3 paper = vec3(0.95, 0.93, 0.88) + vec3(hatch);
                        vec3 ink = vec3(0.11, 0.11, 0.14);
                        float shade = smoothstep(0.12, 0.85, luma(col));
                        vec3 sketched = mix(ink, paper, clamp((1.0 - edge) * (0.45 + 0.55 * shade), 0.0, 1.0));
                        col = mix(col, sketched, fxInt);
                    } else {
                        // Comic Half-Tone rotated dot matrix
                        float angle = 0.5236; // 30 degrees
                        float ca = cos(angle);
                        float sa = sin(angle);
                        vec2 p = vScreenUv * vec2(140.0, 82.0);
                        vec2 rot = vec2(ca * p.x - sa * p.y, sa * p.x + ca * p.y);
                        vec2 cell = fract(rot) - 0.5;
                        float distDot = length(cell);
                        float y = luma(col);
                        float dotRadius = sqrt(clamp(1.0 - y, 0.0, 1.0)) * 0.56;
                        float dotMask = smoothstep(dotRadius - 0.06, dotRadius + 0.06, distDot);
                        vec3 comic = mix(col * 0.22, min(col * 1.18 + 0.06, vec3(1.0)), dotMask);
                        col = mix(col, comic, fxInt);
                    }
                }

                // 3C. Vignette (darken or lighten outer corners)
                if (uVignetteParams.x > 0.5 && abs(uVignetteParams.y) > 0.005) {
                    float d = distance(vScreenUv, vec2(0.5));
                    float mask = smoothstep(0.28, 0.82, d);
                    if (uVignetteParams.y >= 0.0) {
                        col *= (1.0 - mask * uVignetteParams.y * 0.85);
                    } else {
                        col = mix(col, vec3(1.0), mask * (-uVignetteParams.y) * 0.55);
                    }
                }

                // 3I. Letterbox (Top/Bottom & Left/Right cinematic bars)
                if (uLetterboxParams.x > 0.5) {
                    if (vScreenUv.y < uLetterboxParams.y || vScreenUv.y > (1.0 - uLetterboxParams.y) ||
                        vScreenUv.x < uLetterboxParams.z || vScreenUv.x > (1.0 - uLetterboxParams.z)) {
                        col = vec3(0.0);
                    }
                }

                gl_FragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
            }
        """
    }
}
