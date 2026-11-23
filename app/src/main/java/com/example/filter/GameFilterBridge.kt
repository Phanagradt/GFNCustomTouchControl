package com.example.filter

import android.view.Surface
import com.example.model.GameFilterState
import com.example.model.GameFilterStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zero-latency in-memory bridge connecting smooth 120Hz slider drags,
 * the OpenGL ES Freestyle Shader Overlay, and MediaProjection Live Capture.
 */
object GameFilterBridge {

    private val _liveFilterState = MutableStateFlow(GameFilterState.default())
    val liveFilterState: StateFlow<GameFilterState> = _liveFilterState.asStateFlow()

    private val _isGpuShaderActive = MutableStateFlow(false)
    val isGpuShaderActive: StateFlow<Boolean> = _isGpuShaderActive.asStateFlow()

    private val _gpuTargetSurface = MutableStateFlow<Surface?>(null)
    val gpuTargetSurface: StateFlow<Surface?> = _gpuTargetSurface.asStateFlow()

    private val _gpuContentSize = MutableStateFlow(1280 to 720)
    val gpuContentSize: StateFlow<Pair<Int, Int>> = _gpuContentSize.asStateFlow()

    fun syncFromPersisted(state: GameFilterState) {
        _liveFilterState.value = state
    }

    fun updateLiveStyle(style: GameFilterStyle) {
        val current = _liveFilterState.value
        _liveFilterState.value = current
            .withUpdatedStyle(style)
            .copy(activeSlot = style.slot.coerceIn(1, 3))
    }

    fun selectLiveSlot(slot: Int) {
        _liveFilterState.value = _liveFilterState.value.copy(activeSlot = slot.coerceIn(0, 3))
    }

    fun setGpuShaderActive(active: Boolean) {
        _isGpuShaderActive.value = active
        if (!active) {
            _gpuTargetSurface.value = null
        }
    }

    fun publishGpuTargetSurface(surface: Surface?, width: Int = 0, height: Int = 0) {
        if (width > 0 && height > 0) {
            _gpuContentSize.value = width to height
        }
        _gpuTargetSurface.value = surface
    }

    fun updateGpuContentSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            _gpuContentSize.value = width to height
        }
    }
}
