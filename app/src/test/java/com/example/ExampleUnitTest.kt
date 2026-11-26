package com.example

import android.view.KeyEvent
import com.example.input.InputBridge
import com.example.capture.CaptureBridge
import com.example.cloud.CloudLayoutSyncRepository
import com.example.data.ProfileRepository
import com.example.filter.FreestyleGlTextureView
import com.example.filter.GameFilterBridge
import com.example.model.AppColorSchemeConfig
import com.example.model.AppFontConfig
import com.example.model.AppFontPreset
import com.example.model.AppLanguage
import com.example.model.AppLocalization
import com.example.model.AppThemePreset
import com.example.model.AxisPair
import com.example.model.BlackWhiteTone
import com.example.model.CaptureMediaType
import com.example.model.CapturedMediaItem
import com.example.model.ColorMatrixBuilder
import com.example.model.ColorblindMode
import com.example.model.CommandCatalog
import com.example.model.ControllerNotation
import com.example.model.ControllerProfile
import com.example.model.CrosshairPresets
import com.example.model.CrosshairSettings
import com.example.model.CrosshairStyle
import com.example.model.FilterTintPreset
import com.example.model.GameFilterState
import com.example.model.GameFilterStyle
import com.example.model.GameGenre
import com.example.model.MoodFilterPreset
import com.example.model.PresetProfiles
import com.example.model.RecordingConfigHelper
import com.example.model.RecordingFps
import com.example.model.RecordingResolution
import com.example.model.ReplayBufferPreset
import com.example.model.SpecialFxMode
import com.example.model.StickerPreset
import com.example.model.TargetPlatformMode
import android.view.Surface
import com.example.data.CloudAppearanceSnapshot
import com.example.input.GyroAimEngine
import com.example.model.FloatingBubbleConfig
import com.example.model.FloatingBubbleStyle
import com.example.model.GyroActivationMode
import com.example.model.GyroAimConfig
import com.example.model.GfnRegionPreset
import com.example.model.PerfHudConfig
import com.example.model.TouchpadMode
import com.example.model.WebPlayerUserAgentMode
import com.example.model.WebStreamQualityConfig
import com.example.service.KeyboardWordPredictor
import com.example.service.PerformanceMonitor
import com.example.ui.components.computeOverlayBlurRadius
import com.example.ui.components.computeOverlayDimAmount
import com.example.update.GitHubUpdateManager
import com.example.webplayer.BetterGfnScriptEngine
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExampleUnitTest {

    @Before
    fun setup() {
        InputBridge.attachImeSink(null)
        InputBridge.attachPointerSink(null)
        InputBridge.setAppForeground(false)
        InputBridge.setTargetMode(TargetPlatformMode.GEFORCE_NOW)
        InputBridge.releaseAll()
        InputBridge.clearTelemetry()
        CaptureBridge.setInstantReplayOn(false)
        CaptureBridge.setRecordingState(false, 0L)
        CaptureBridge.setReplayDurationSec(30)
        CaptureBridge.postBanner(null)
    }

    @Test
    fun `starter presets contain all five GeForce NOW layouts including PC Mouse`() {
        val presets = PresetProfiles.createStarterProfiles()
        assertEquals(5, presets.size)
        assertTrue(presets.first().isActive)
        assertTrue(presets.any { it.id == "preset_pc_mouse_wasd" })
        assertTrue(presets.all { it.controls.isNotEmpty() })
    }

    @Test
    fun `command catalog formats Xbox and PlayStation notations accurately`() {
        val btnA = CommandCatalog.findById("btn_a")
        val btnY = CommandCatalog.findById("btn_y")
        assertEquals("A", CommandCatalog.formatFaceLabel(btnA, ControllerNotation.XBOX))
        assertEquals("✕", CommandCatalog.formatFaceLabel(btnA, ControllerNotation.PLAYSTATION))
        assertEquals("△", CommandCatalog.formatFaceLabel(btnY, ControllerNotation.PLAYSTATION))
    }

    @Test
    fun `moshi serializes and deserializes controller profile round-trip`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(ControllerProfile::class.java)
        val original = PresetProfiles.createBlankProfile("Test FPS", "Apex Legends", GameGenre.FPS)

        val json = adapter.toJson(original)
        val decoded = adapter.fromJson(json)
        assertNotNull(decoded)
        assertEquals(original.name, decoded?.name)
        assertEquals(original.controls.size, decoded?.controls?.size)
        assertEquals(original.genre, decoded?.genre)
    }

    @Test
    fun `analog stick deadzone filters drift and toggle latches cleanly`() {
        InputBridge.updateAnalogStick(
            axisPair = AxisPair.LEFT_STICK,
            sourceLabel = "Move",
            rawX = 0.04f,
            rawY = -0.04f,
            deadzone = 0.12f,
            sensitivity = 1.0f,
            active = true
        )
        assertEquals(0f, InputBridge.leftStick.value.x, 0.001f)
        assertEquals(0f, InputBridge.leftStick.value.y, 0.001f)

        InputBridge.updateAnalogStick(
            axisPair = AxisPair.RIGHT_STICK,
            sourceLabel = "Aim",
            rawX = 0.80f,
            rawY = 0.0f,
            deadzone = 0.10f,
            sensitivity = 1.2f,
            active = true
        )
        assertTrue(InputBridge.rightStick.value.x > 0.85f)

        val lt = CommandCatalog.findById("btn_lt")
        assertTrue(InputBridge.toggleControl("toggle_lt", "AIM", lt))
        assertTrue("toggle_lt" in InputBridge.toggledControlIds.value)
        assertFalse(InputBridge.toggleControl("toggle_lt", "AIM", lt))
        assertFalse("toggle_lt" in InputBridge.toggledControlIds.value)
    }

    @Test
    fun `wasd cluster records directional press and release telemetry`() {
        InputBridge.updateDirectionalCluster(
            controlId = "wasd_1",
            sourceLabel = "WASD",
            axisPair = AxisPair.WASD_KEYS,
            x = 0.7f,
            y = -0.8f,
            active = true
        )
        val downEvents = InputBridge.telemetry.value.map { it.codeLabel }
        assertTrue(downEvents.contains("W"))
        assertTrue(downEvents.contains("D"))

        InputBridge.updateDirectionalCluster(
            controlId = "wasd_1",
            sourceLabel = "WASD",
            axisPair = AxisPair.WASD_KEYS,
            x = 0f,
            y = 0f,
            active = false
        )
        val upEvents = InputBridge.telemetry.value.filter { it.actionName == "KEY_UP" }.map { it.codeLabel }
        assertTrue(upEvents.contains("W"))
        assertTrue(upEvents.contains("D"))
    }

    @Test
    fun `toggled shift persists metaState across WASD direction turns and clears cleanly`() {
        val shift = CommandCatalog.findById("key_shift")
        val keyW = CommandCatalog.findById("key_w")
        val keyD = CommandCatalog.findById("key_d")
        val expectedShiftMeta = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON

        // 1. Walk forward (W) then toggle Shift ON
        InputBridge.updateDirectionalCluster("wasd_1", "WASD", AxisPair.WASD_KEYS, x = 0f, y = -0.9f, active = true)
        assertTrue(InputBridge.toggleControl("toggle_shift", "SPRINT", shift))
        assertEquals(expectedShiftMeta, InputBridge.currentEffectiveMetaState())

        // 2. Turn right (W -> D): new key D must press before W releases, and Shift metaState stays active
        InputBridge.clearTelemetry()
        InputBridge.updateDirectionalCluster("wasd_1", "WASD", AxisPair.WASD_KEYS, x = 0.9f, y = 0f, active = true)
        assertEquals(expectedShiftMeta, InputBridge.currentEffectiveMetaState())
        val transitionEvents = InputBridge.telemetry.value.reversed() // chronological
        assertEquals("D", transitionEvents[0].codeLabel)
        assertEquals("KEY_DOWN", transitionEvents[0].actionName)
        assertEquals("W", transitionEvents[1].codeLabel)
        assertEquals("KEY_UP", transitionEvents[1].actionName)

        // 3. Individual button press/release (W/D) while Shift is toggled keeps Shift metaState active
        InputBridge.pressControl("btn_d", "D", keyD)
        InputBridge.releaseControl("btn_d", "D", keyD)
        InputBridge.pressControl("btn_w", "W", keyW)
        InputBridge.releaseControl("btn_w", "W", keyW)
        assertEquals(expectedShiftMeta, InputBridge.currentEffectiveMetaState())

        // 4. Toggle Shift OFF clears metaState
        assertFalse(InputBridge.toggleControl("toggle_shift", "SPRINT", shift))
        assertEquals(0, InputBridge.currentEffectiveMetaState())

        // 5. Re-toggle ON then releaseAll() clears both toggle state and modifier bits
        assertTrue(InputBridge.toggleControl("toggle_shift", "SPRINT", shift))
        assertEquals(expectedShiftMeta, InputBridge.currentEffectiveMetaState())
        InputBridge.releaseAll()
        assertEquals(0, InputBridge.currentEffectiveMetaState())
        assertTrue(InputBridge.toggledControlIds.value.isEmpty())
    }

    @Test
    fun `touchpad camera look dispatches Q and E turn keys and respects invertY`() {
        InputBridge.clearTelemetry()
        InputBridge.updateTouchpadLook(
            controlId = "campad_1",
            sourceLabel = "LOOK PAD",
            axisPair = AxisPair.TOUCHPAD_QE,
            normX = 0.85f,
            normY = -0.80f,
            deadzone = 0.10f,
            invertY = false,
            active = true
        )
        val codes = InputBridge.telemetry.value.filter { it.actionName == "KEY_DOWN" }.map { it.codeLabel }
        assertTrue("E" in codes)

        InputBridge.clearTelemetry()
        InputBridge.updateTouchpadLook(
            controlId = "campad_1",
            sourceLabel = "LOOK PAD",
            axisPair = AxisPair.TOUCHPAD_QE,
            normX = 0f,
            normY = -0.80f,
            deadzone = 0.10f,
            invertY = true,
            active = true
        )
        assertTrue(InputBridge.rightStick.value.y > 0.7f)
        InputBridge.updateTouchpadLook(
            controlId = "campad_1",
            sourceLabel = "LOOK PAD",
            axisPair = AxisPair.TOUCHPAD_QE,
            normX = 0f,
            normY = 0f,
            deadzone = 0.10f,
            invertY = true,
            active = false
        )
    }

    @Test
    fun `regular mouse buttons and scroll dispatch to PointerGestureSink cleanly`() {
        val events = mutableListOf<String>()
        InputBridge.attachPointerSink(object : InputBridge.PointerGestureSink {
            override fun onLeftButton(isDown: Boolean) {
                events += if (isDown) "LMB_DOWN" else "LMB_UP"
            }

            override fun onRightButton(isDown: Boolean) {
                events += if (isDown) "RMB_DOWN" else "RMB_UP"
            }

            override fun onMiddleClick() {
                events += "MMB_CLICK"
            }

            override fun onScroll(scrollUp: Boolean) {
                events += if (scrollUp) "SCROLL_UP" else "SCROLL_DOWN"
            }
        })

        val lmb = CommandCatalog.findById("mouse_left")
        val rmb = CommandCatalog.findById("mouse_right")
        val mmb = CommandCatalog.findById("mouse_middle")
        val scrUp = CommandCatalog.findById("mouse_scroll_up")
        val scrDown = CommandCatalog.findById("mouse_scroll_down")

        InputBridge.pressControl("btn_lmb", "LMB", lmb)
        InputBridge.releaseControl("btn_lmb", "LMB", lmb)
        InputBridge.pressControl("btn_rmb", "RMB", rmb)
        InputBridge.releaseControl("btn_rmb", "RMB", rmb)
        InputBridge.pressControl("btn_mmb", "MMB", mmb)
        InputBridge.pressControl("btn_scrup", "SCR ↑", scrUp)
        InputBridge.pressControl("btn_scrdn", "SCR ↓", scrDown)

        assertEquals(
            listOf("LMB_DOWN", "LMB_UP", "RMB_DOWN", "RMB_UP", "MMB_CLICK", "SCROLL_UP", "SCROLL_DOWN"),
            events
        )
    }

    @Test
    fun `custom combo builder creates Shift plus E chord and PC Mouse preset includes regular mouse binds`() {
        val shift = CommandCatalog.findById("key_shift")
        val keyE = CommandCatalog.findById("key_e")
        val combo = CommandCatalog.createCustomComboBinding(shift, keyE)
        assertEquals("SHF+E", combo.codeLabel)
        assertEquals(KeyEvent.KEYCODE_E, combo.primaryKeyCode)
        assertEquals(KeyEvent.KEYCODE_SHIFT_LEFT, combo.secondaryKeyCode)
        assertEquals(KeyEvent.META_SHIFT_ON, combo.modifierMetaState)

        val mouseProfile = PresetProfiles.createProfileFromTemplate(
            name = "DBD Mouse",
            gameTitle = "Dead by Daylight",
            genre = GameGenre.FPS,
            template = com.example.model.ProfileTemplate.PC_MOUSE_WASD
        )
        val ids = mouseProfile.controls.map { it.binding.id }.toSet()
        assertTrue("mouse_left" in ids)
        assertTrue("mouse_right" in ids)
        assertTrue("mouse_middle" in ids)
        assertTrue("mouse_scroll_up" in ids)
        assertTrue("mouse_scroll_down" in ids)
        assertEquals("mouse_left", CommandCatalog.findById("mouse_m1").id)
    }

    @Test
    fun `linux evdev scan codes prevent keycode collisions like A vs LeftCtrl`() {
        assertEquals(30, InputBridge.resolveLinuxScanCode(KeyEvent.KEYCODE_A, com.example.model.BindingDomain.KEYBOARD))
        assertEquals(29, InputBridge.resolveLinuxScanCode(KeyEvent.KEYCODE_CTRL_LEFT, com.example.model.BindingDomain.KEYBOARD))
        assertEquals(17, InputBridge.resolveLinuxScanCode(KeyEvent.KEYCODE_W, com.example.model.BindingDomain.KEYBOARD))
        assertEquals(31, InputBridge.resolveLinuxScanCode(KeyEvent.KEYCODE_S, com.example.model.BindingDomain.KEYBOARD))
        assertEquals(32, InputBridge.resolveLinuxScanCode(KeyEvent.KEYCODE_D, com.example.model.BindingDomain.KEYBOARD))
        assertEquals(311, InputBridge.resolveLinuxScanCode(KeyEvent.KEYCODE_BUTTON_R1, com.example.model.BindingDomain.CONTROLLER))
        assertEquals(313, InputBridge.resolveLinuxScanCode(KeyEvent.KEYCODE_BUTTON_R2, com.example.model.BindingDomain.CONTROLLER))
        assertEquals(103, InputBridge.resolveLinuxScanCode(KeyEvent.KEYCODE_DPAD_UP, com.example.model.BindingDomain.KEYBOARD))
        assertEquals(544, InputBridge.resolveLinuxScanCode(KeyEvent.KEYCODE_DPAD_UP, com.example.model.BindingDomain.CONTROLLER))
    }

    @Test
    fun `moshi preserves custom button image base64 across profile serialization`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(ControllerProfile::class.java)
        val baseProfile = PresetProfiles.createBlankProfile("Art Pad", "Fortnite", GameGenre.FPS)
        val updatedControls = baseProfile.controls.mapIndexed { index, ctrl ->
            if (index == 0) ctrl.copy(customImageBase64 = "UklGRg==") else ctrl
        }
        val profile = baseProfile.copy(controls = updatedControls)

        val json = adapter.toJson(profile)
        val restored = adapter.fromJson(json)
        assertNotNull(restored)
        assertEquals("UklGRg==", restored?.controls?.firstOrNull()?.customImageBase64)
    }

    @Test
    fun `dispatching key A omits FLAG_SOFT_KEYBOARD so scanCode 30 reaches cloud client`() {
        assertEquals(0, InputBridge.effectiveDispatchFlags() and KeyEvent.FLAG_SOFT_KEYBOARD)
        assertEquals(KeyEvent.FLAG_KEEP_TOUCH_MODE, InputBridge.effectiveDispatchFlags())

        val dispatched = mutableListOf<KeyEvent>()
        InputBridge.attachImeSink { ev -> dispatched += ev }

        val keyA = CommandCatalog.findById("key_a")
        InputBridge.pressControl("test_a", "A", keyA)
        InputBridge.releaseControl("test_a", "A", keyA)

        assertEquals(2, dispatched.size)
        val telemetry = InputBridge.telemetry.value.firstOrNull()
        assertNotNull(telemetry)
        assertTrue(telemetry!!.detail.contains("scan=30"))
    }

    @Test
    fun `every single button command maps to a non-zero linux evdev scan code`() {
        CommandCatalog.singleButtonCommands.forEach { cmd ->
            val scan = InputBridge.resolveLinuxScanCode(cmd.primaryKeyCode, cmd.domain)
            assertTrue("Missing Linux scanCode for ${cmd.id} (${cmd.codeLabel})", scan > 0)
        }
    }

    @Test
    fun `game filter state serializes round-trip and resets active style cleanly`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(GameFilterState::class.java)

        val initial = GameFilterState.default()
        assertEquals(0, initial.activeSlot)
        assertEquals(3, initial.styles.size)
        assertFalse(initial.isEnabled)

        val edited = initial.copy(activeSlot = 2).withUpdatedStyle(
            GameFilterStyle(
                slot = 2,
                name = "Vivid HDR",
                brightness = 0.25f,
                contrast = 0.30f,
                shadows = 0.55f,
                colorVibrance = 0.45f,
                temperature = -0.15f,
                tintPreset = FilterTintPreset.COOL_CYAN,
                tintIntensity = 0.22f
            )
        )
        assertTrue(edited.isEnabled)

        val decoded = adapter.fromJson(adapter.toJson(edited))
        assertNotNull(decoded)
        assertEquals(2, decoded?.activeSlot)
        assertEquals(0.55f, decoded?.activeStyle?.shadows ?: 0f, 0.001f)
        assertEquals(FilterTintPreset.COOL_CYAN, decoded?.activeStyle?.tintPreset)

        val reset = decoded!!.withResetSlot(2)
        assertEquals(0f, reset.styleForSlot(2).brightness, 0.001f)
        assertEquals(0f, reset.styleForSlot(2).shadows, 0.001f)
        assertEquals(FilterTintPreset.NEUTRAL, reset.styleForSlot(2).tintPreset)
    }

    @Test
    fun `color matrix builder produces identity for flat style and lifts shadows or dims cleanly`() {
        val neutral = GameFilterStyle(slot = 1, name = "Flat")
        val idMatrix = ColorMatrixBuilder.buildColorMatrixValues(neutral)
        assertEquals(20, idMatrix.size)
        assertEquals(1f, idMatrix[0], 0.001f)
        assertEquals(0f, idMatrix[4], 0.001f)
        assertEquals(1f, idMatrix[6], 0.001f)
        assertEquals(1f, idMatrix[12], 0.001f)

        val liftedShadows = neutral.copy(shadows = 0.60f, brightness = 0.20f)
        val liftedMatrix = ColorMatrixBuilder.buildColorMatrixValues(liftedShadows)
        assertTrue(liftedMatrix[4] > 40f)
        assertTrue(liftedMatrix[9] > 40f)
        assertTrue(liftedMatrix[14] > 40f)

        val warmTint = neutral.copy(
            temperature = 0.40f,
            tintPreset = FilterTintPreset.WARM_AMBER,
            tintIntensity = 0.30f
        )
        val warmMatrix = ColorMatrixBuilder.buildColorMatrixValues(warmTint)
        assertTrue(warmMatrix[4] > warmMatrix[14])

        assertEquals(0f, computeOverlayDimAmount(liftedShadows), 0.001f)
        assertTrue(computeOverlayDimAmount(neutral.copy(brightness = -0.35f)) > 0.2f)
    }

    @Test
    fun `crosshair settings presets apply cleanly and serialize round-trip`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(CrosshairSettings::class.java)

        val base = CrosshairSettings(enabled = true)
        val haloPreset = CrosshairPresets.presets.first { it.id == "holo_ring" }
        val styled = haloPreset.applyTo(base).copy(
            gapDp = 8.5f,
            thicknessDp = 3.5f,
            outlineEnabled = true,
            outlineThicknessDp = 2.0f
        )

        val decoded = adapter.fromJson(adapter.toJson(styled))
        assertNotNull(decoded)
        assertEquals(CrosshairStyle.CIRCLE_DOT, decoded?.style)
        assertEquals(8.5f, decoded?.gapDp ?: 0f, 0.001f)
        assertEquals(3.5f, decoded?.thicknessDp ?: 0f, 0.001f)
        assertTrue(decoded?.outlineEnabled == true)
    }

    @Test
    fun `app localization auto-detects Thai system locale and supports explicit override`() {
        val thaiLocale = Locale.forLanguageTag("th-TH")
        val spanishLocale = Locale.forLanguageTag("es-ES")
        val autoThai = AppLocalization.resolveEffectiveLanguage(AppLanguage.SYSTEM, thaiLocale)
        assertEquals(AppLanguage.THAI, autoThai)
        val thaiStrings = AppLocalization.stringsFor(AppLanguage.SYSTEM, thaiLocale)
        assertEquals("เป้าเล็ง", thaiStrings.menuCrosshair)
        assertEquals("คีย์บอร์ดลอย", thaiStrings.menuFloatKb)

        val autoSpanish = AppLocalization.resolveEffectiveLanguage(AppLanguage.SYSTEM, spanishLocale)
        assertEquals(AppLanguage.SPANISH, autoSpanish)

        val overrideEnglish = AppLocalization.stringsFor(AppLanguage.ENGLISH, thaiLocale)
        assertEquals("Crosshair", overrideEnglish.menuCrosshair)
    }

    @Test
    fun `mouse touchpad flick dispatches mouse swipe deltas and dual mouse KeyEvents`() {
        val swipes = mutableListOf<Pair<Float, Float>>()
        val pointerButtons = mutableListOf<String>()
        val dispatchedEvents = mutableListOf<KeyEvent>()
        InputBridge.attachImeSink { ev -> dispatchedEvents += ev }
        InputBridge.attachPointerSink(object : InputBridge.PointerGestureSink {
            override fun onLeftButton(isDown: Boolean) {
                pointerButtons += if (isDown) "LMB_DOWN" else "LMB_UP"
            }
            override fun onRightButton(isDown: Boolean) {
                pointerButtons += if (isDown) "RMB_DOWN" else "RMB_UP"
            }
            override fun onMiddleClick() {
                pointerButtons += "MMB_CLICK"
            }
            override fun onScroll(scrollUp: Boolean) = Unit
            override fun onMouseSwipe(dxPx: Float, dyPx: Float) {
                swipes += dxPx to dyPx
            }
        })

        // Verify mouse touchpad defaults to TouchpadMode.MOUSE
        val mouseProfile = PresetProfiles.createProfileFromTemplate(
            name = "Flick Test",
            gameTitle = "CS2",
            genre = GameGenre.FPS,
            template = com.example.model.ProfileTemplate.PC_MOUSE_WASD
        )
        val pad = mouseProfile.controls.first { it.controlType == com.example.model.ControlType.TOUCHPAD }
        assertEquals(TouchpadMode.MOUSE, pad.touchpadMode)

        InputBridge.updateTouchpadMouseMove(
            controlId = pad.id,
            sourceLabel = pad.label,
            axisPair = pad.binding.axisPair,
            deltaXPx = 24f,
            deltaYPx = -10f,
            sensitivity = 1.5f,
            invertY = false,
            isFlickInertia = true,
            active = true
        )
        assertEquals(1, swipes.size)
        assertEquals(36f, swipes[0].first, 0.01f)
        assertEquals(-15f, swipes[0].second, 0.01f)

        val lmb = CommandCatalog.findById("mouse_left")
        val rmb = CommandCatalog.findById("mouse_right")
        val mmb = CommandCatalog.findById("mouse_middle")
        InputBridge.clearTelemetry()
        dispatchedEvents.clear()
        InputBridge.pressControl("m1", "LMB", lmb)
        InputBridge.pressControl("m2", "RMB", rmb)
        InputBridge.pressControl("m3", "MMB", mmb)

        assertEquals(3, dispatchedEvents.size)
        assertEquals(listOf("LMB_DOWN", "RMB_DOWN", "MMB_CLICK"), pointerButtons)
        val telemetryDetails = InputBridge.telemetry.value.map { it.detail }
        assertTrue(telemetryDetails.any { it.contains("scan=272") })
        assertTrue(telemetryDetails.any { it.contains("scan=273") })
        assertTrue(telemetryDetails.any { it.contains("scan=274") })
        assertEquals(272, InputBridge.resolveLinuxScanCode(KeyEvent.KEYCODE_BUTTON_1, com.example.model.BindingDomain.MOUSE))
        assertEquals(273, InputBridge.resolveLinuxScanCode(KeyEvent.KEYCODE_BUTTON_2, com.example.model.BindingDomain.MOUSE))
        assertEquals(274, InputBridge.resolveLinuxScanCode(KeyEvent.KEYCODE_BUTTON_3, com.example.model.BindingDomain.MOUSE))
    }

    @Test
    fun `foreground play-test preserves LMB RMB MMB MOUSE_DOWN state and skips self-tapping PointerGestureSink`() {
        val pointerCalls = mutableListOf<String>()
        InputBridge.attachPointerSink(object : InputBridge.PointerGestureSink {
            override fun onLeftButton(isDown: Boolean) {
                pointerCalls += if (isDown) "LMB_DOWN" else "LMB_UP"
            }

            override fun onRightButton(isDown: Boolean) {
                pointerCalls += if (isDown) "RMB_DOWN" else "RMB_UP"
            }

            override fun onMiddleClick() {
                pointerCalls += "MMB_CLICK"
            }

            override fun onScroll(scrollUp: Boolean) = Unit
        })

        InputBridge.setAppForeground(true)

        val lmb = CommandCatalog.findById("mouse_left")
        val rmb = CommandCatalog.findById("mouse_right")
        val mmb = CommandCatalog.findById("mouse_middle")

        InputBridge.pressControl("btn_lmb", "LMB", lmb)
        assertTrue("btn_lmb" in InputBridge.pressedControlIds.value)
        assertEquals("MOUSE_DOWN", InputBridge.telemetry.value.first().actionName)
        assertEquals("LMB", InputBridge.telemetry.value.first().codeLabel)
        InputBridge.releaseControl("btn_lmb", "LMB", lmb)
        assertFalse("btn_lmb" in InputBridge.pressedControlIds.value)
        assertEquals("MOUSE_UP", InputBridge.telemetry.value.first().actionName)

        InputBridge.pressControl("btn_rmb", "RMB", rmb)
        assertEquals("MOUSE_DOWN", InputBridge.telemetry.value.first().actionName)
        assertEquals("RMB", InputBridge.telemetry.value.first().codeLabel)
        InputBridge.releaseControl("btn_rmb", "RMB", rmb)
        assertEquals("MOUSE_UP", InputBridge.telemetry.value.first().actionName)

        InputBridge.pressControl("btn_mmb", "MMB", mmb)
        assertEquals("MOUSE_DOWN", InputBridge.telemetry.value.first().actionName)
        assertEquals("MMB", InputBridge.telemetry.value.first().codeLabel)
        InputBridge.releaseControl("btn_mmb", "MMB", mmb)
        assertEquals("MOUSE_UP", InputBridge.telemetry.value.first().actionName)

        assertTrue("PointerGestureSink must not self-tap while app is in foreground", pointerCalls.isEmpty())
    }

    @Test
    fun `continuous mouse touchpad streams unbroken swipe lifecycle without interrupting held keyboard keys`() {
        val imeEvents = mutableListOf<KeyEvent>()
        val swipeEvents = mutableListOf<String>()
        InputBridge.attachImeSink { ev -> imeEvents += ev }
        InputBridge.attachPointerSink(object : InputBridge.PointerGestureSink {
            override fun onLeftButton(isDown: Boolean) = Unit
            override fun onRightButton(isDown: Boolean) = Unit
            override fun onMiddleClick() = Unit
            override fun onScroll(scrollUp: Boolean) = Unit
            override fun onMouseSwipeStart() {
                swipeEvents += "START"
            }
            override fun onMouseSwipe(dxPx: Float, dyPx: Float) {
                swipeEvents += "MOVE(${dxPx.toInt()},${dyPx.toInt()})"
            }
            override fun onMouseSwipeEnd() {
                swipeEvents += "END"
            }
        })

        // Hold W key while panning camera continuously with touchpad
        val keyW = CommandCatalog.findById("key_w")
        InputBridge.pressControl("btn_w", "W", keyW)
        imeEvents.clear()

        InputBridge.startTouchpadMouseMove()
        InputBridge.updateTouchpadMouseMove(
            controlId = "pad_1",
            sourceLabel = "CAMERA",
            axisPair = AxisPair.TOUCHPAD_ARROWS,
            deltaXPx = 12f,
            deltaYPx = -4f,
            sensitivity = 1.0f,
            invertY = false,
            isFlickInertia = false,
            active = true
        )
        InputBridge.updateTouchpadMouseMove(
            controlId = "pad_1",
            sourceLabel = "CAMERA",
            axisPair = AxisPair.TOUCHPAD_ARROWS,
            deltaXPx = 8f,
            deltaYPx = 2f,
            sensitivity = 1.0f,
            invertY = false,
            isFlickInertia = true,
            active = true
        )
        InputBridge.endTouchpadMouseMove()

        assertEquals(listOf("START", "MOVE(12,-4)", "MOVE(8,2)", "END"), swipeEvents)
        assertTrue("Mouse trackpad must not emit KeyEvents that interrupt held W/Shift keys", imeEvents.isEmpty())
        assertTrue("W must remain pressed throughout camera panning", "btn_w" in InputBridge.pressedControlIds.value)
    }

    @Test
    fun `target platform mode switches cleanly and has localized strings`() {
        InputBridge.setTargetMode(TargetPlatformMode.ANDROID_NATIVE)
        assertEquals(TargetPlatformMode.ANDROID_NATIVE, InputBridge.targetMode.value)
        assertEquals("Android", InputBridge.targetMode.value.shortLabel)

        val en = AppLocalization.stringsFor(AppLanguage.ENGLISH)
        val th = AppLocalization.stringsFor(AppLanguage.THAI)
        assertTrue(en.targetModeAndroidTitle.contains("Roblox"))
        assertTrue(th.targetModeAndroidTitle.contains("Roblox"))
    }

    @Test
    fun `overlay touch guard defers LMB RMB MMB and flick accessibility gestures until finger lifts`() {
        val pointerEvents = mutableListOf<String>()
        InputBridge.attachPointerSink(object : InputBridge.PointerGestureSink {
            override fun onLeftButton(isDown: Boolean) {
                pointerEvents += if (isDown) "LMB_DOWN" else "LMB_UP"
            }

            override fun onRightButton(isDown: Boolean) {
                pointerEvents += if (isDown) "RMB_DOWN" else "RMB_UP"
            }

            override fun onMiddleClick() {
                pointerEvents += "MMB_CLICK"
            }

            override fun onScroll(scrollUp: Boolean) {
                pointerEvents += if (scrollUp) "SCROLL_UP" else "SCROLL_DOWN"
            }

            override fun onMouseFlick(dxPx: Float, dyPx: Float) {
                pointerEvents += "FLICK(${dxPx.toInt()},${dyPx.toInt()})"
            }
        })

        val lmb = CommandCatalog.findById("mouse_left")
        val rmb = CommandCatalog.findById("mouse_right")
        val mmb = CommandCatalog.findById("mouse_middle")

        // Finger presses LMB on overlay -> IME/pressed state updates immediately, PointerGestureSink is deferred
        InputBridge.beginOverlayTouch()
        InputBridge.pressControl("btn_lmb", "LMB", lmb)
        assertTrue("btn_lmb" in InputBridge.pressedControlIds.value)
        assertTrue("Pointer gesture must not fire mid-touch on overlay", pointerEvents.isEmpty())

        // Finger releases LMB -> deferred LeftTap flushes cleanly after endOverlayTouch
        InputBridge.releaseControl("btn_lmb", "LMB", lmb)
        InputBridge.endOverlayTouch()
        assertFalse("btn_lmb" in InputBridge.pressedControlIds.value)
        assertEquals(listOf("LMB_DOWN", "LMB_UP"), pointerEvents)

        // Trackpad drag followed by release commits FlickSwipe only once overlay fingers lift
        pointerEvents.clear()
        InputBridge.beginOverlayTouch() // Trackpad finger down
        InputBridge.beginOverlayTouch() // RMB finger down
        InputBridge.pressControl("btn_rmb", "RMB", rmb)
        assertTrue("btn_rmb" in InputBridge.pressedControlIds.value)
        assertTrue(pointerEvents.isEmpty())

        InputBridge.releaseControl("btn_rmb", "RMB", rmb)
        InputBridge.endOverlayTouch() // RMB finger lifts, Trackpad finger still down
        assertTrue("Deferred RMB click waits for Trackpad finger too", pointerEvents.isEmpty())

        // Single-finger Trackpad lift flushes deferred FlickSwipe without canceling mid-drag
        InputBridge.commitTouchpadFlickSwipe("CAMERA", 42f, -18f, 1.0f, false)
        assertTrue(pointerEvents.isEmpty())

        InputBridge.endOverlayTouch() // Last finger lifts -> FlickSwipe flushes cleanly
        assertEquals(listOf("FLICK(42,-18)"), pointerEvents)

        // Quick MMB verification
        pointerEvents.clear()
        InputBridge.beginOverlayTouch()
        InputBridge.pressControl("btn_mmb", "MMB", mmb)
        assertTrue("btn_mmb" in InputBridge.pressedControlIds.value)
        assertTrue(pointerEvents.isEmpty())
        InputBridge.releaseControl("btn_mmb", "MMB", mmb)
        InputBridge.endOverlayTouch()
        assertEquals(listOf("MMB_CLICK"), pointerEvents)
    }

    @Test
    fun `replay buffer presets format 10s to 1h and captured media item round-trips`() {
        assertEquals("10s", ReplayBufferPreset.formatDuration(10))
        assertEquals("20s", ReplayBufferPreset.formatDuration(20))
        assertEquals("30s", ReplayBufferPreset.formatDuration(30))
        assertEquals("1m", ReplayBufferPreset.formatDuration(60))
        assertEquals("30m", ReplayBufferPreset.formatDuration(1800))
        assertEquals("1h", ReplayBufferPreset.formatDuration(3600))
        assertEquals("1h", ReplayBufferPreset.formatDuration(9999))
        assertEquals("10s", ReplayBufferPreset.formatDuration(2))

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(CapturedMediaItem::class.java)
        val item = CapturedMediaItem(
            id = "clip-1",
            filePath = "/data/user/0/com.example/files/nexus_gallery/replay_1.mp4",
            fileName = "replay_1.mp4",
            type = CaptureMediaType.REPLAY,
            createdAtMs = 1_760_000_000_000L,
            durationMs = 20_000L,
            sizeBytes = 4_250_000L,
            replayWindowSec = 20
        )
        val restored = adapter.fromJson(adapter.toJson(item))
        assertNotNull(restored)
        assertEquals(CaptureMediaType.REPLAY, restored?.type)
        assertEquals(20, restored?.replayWindowSec)
        assertEquals(20_000L, restored?.durationMs)

        assertEquals("00:20", CaptureBridge.formatDurationMs(20_000L))
        assertEquals("1:00:00", CaptureBridge.formatDurationMs(3_600_000L))
    }

    @Test
    fun `gallery capture and floating layout chooser strings exist across locales`() {
        val en = AppLocalization.stringsFor(AppLanguage.ENGLISH)
        val th = AppLocalization.stringsFor(AppLanguage.THAI)
        assertEquals("Gallery", en.navGallery)
        assertEquals("แกลเลอรี", th.navGallery)
        assertEquals("Layouts", en.menuLayouts)
        assertEquals("เลือกรูปแบบ", th.menuLayouts)
        assertEquals("Screenshot", en.menuScreenshot)
        assertEquals("Instant Replay", en.menuReplay)
        assertEquals("Save Replay", en.menuSaveReplay)
        assertEquals("Record", en.menuRecord)
        assertEquals("Stop Rec", en.menuStopRecord)
    }

    @Test
    fun `all 18 Freestyle filter modules serialize round-trip and count active modules accurately`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(GameFilterStyle::class.java)

        val fullStyle = GameFilterStyle(
            slot = 1,
            name = "Full 18 Suite",
            sharpenEnabled = true,
            sharpenAmount = 0.65f,
            sharpenIgnoreFilmGrain = 0.25f,
            sharpenPlusEnabled = true,
            sharpenPlusIntensity = 0.50f,
            sharpenPlusTextureDetails = 0.70f,
            brightnessContrastEnabled = true,
            exposure = 0.15f,
            contrast = 0.25f,
            highlights = -0.20f,
            shadows = 0.45f,
            gamma = 0.10f,
            detailsEnabled = true,
            detailsSharpen = 0.30f,
            detailsClarity = 0.40f,
            detailsHdrToning = 0.55f,
            detailsBloom = -0.25f,
            colorEnabled = true,
            tintHue = 195f,
            tintIntensity = 0.30f,
            temperature = -0.10f,
            colorVibrance = 0.60f,
            colorblindEnabled = true,
            colorblindMode = ColorblindMode.DEUTERANOPIA,
            colorblindIntensity = 0.80f,
            nightModeEnabled = true,
            nightModeIntensity = 0.35f,
            splitscreenEnabled = true,
            splitscreenPosition = 0.42f,
            blackWhiteEnabled = true,
            blackWhiteTone = BlackWhiteTone.WARM,
            blackWhiteIntensity = 0.50f,
            sepiaEnabled = true,
            sepiaIntensity = 0.40f,
            vignetteEnabled = true,
            vignetteIntensity = 0.55f,
            dofEnabled = true,
            dofBlurCurve = 0.60f,
            dofFocusDepth = 0.48f,
            dofNearBlur = 0.40f,
            dofFarBlur = 0.70f,
            oldFilmEnabled = true,
            oldFilmGamma = 0.20f,
            oldFilmExposure = -0.10f,
            oldFilmContrast = 0.30f,
            oldFilmVignette = 0.50f,
            oldFilmStrength = 0.75f,
            oldFilmDirtStrength = 0.65f,
            moodEnabled = true,
            moodPreset = MoodFilterPreset.CYBER_NEON,
            moodIntensity = 0.60f,
            specialFxEnabled = true,
            specialFxMode = SpecialFxMode.RETRO,
            specialFxIntensity = 0.50f,
            painterlyEnabled = true,
            painterlyEdgeSharpening = 0.45f,
            painterlyFilterRadius = 0.55f,
            painterlyEdgeThreshold = 0.35f,
            letterboxEnabled = true,
            letterboxHeight = 0.12f,
            letterboxWidth = 0.05f,
            stickersEnabled = true,
            stickerPreset = StickerPreset.TACTICAL_HUD,
            stickerScale = 1.1f,
            stickerOpacity = 0.85f
        )

        assertEquals(18, fullStyle.activeModuleCount())
        val restored = adapter.fromJson(adapter.toJson(fullStyle))
        assertNotNull(restored)
        assertEquals(18, restored!!.activeModuleCount())
        assertEquals(ColorblindMode.DEUTERANOPIA, restored.colorblindMode)
        assertEquals(BlackWhiteTone.WARM, restored.blackWhiteTone)
        assertEquals(MoodFilterPreset.CYBER_NEON, restored.moodPreset)
        assertEquals(SpecialFxMode.RETRO, restored.specialFxMode)
        assertEquals(StickerPreset.TACTICAL_HUD, restored.stickerPreset)
        assertEquals(0.42f, restored.splitscreenPosition, 0.001f)
        assertEquals(0.65f, restored.oldFilmDirtStrength, 0.001f)
    }

    @Test
    fun `daltonization colorblind shifts RGB channels and blur radius respects GPU mode`() {
        val (pr, pg, pb) = ColorMatrixBuilder.applyDaltonization(0.9f, 0.2f, 0.1f, ColorblindMode.PROTANOPIA, 1.0f)
        assertTrue(
            "Protanopia Daltonization must redistribute red channel contrast",
            kotlin.math.abs(pg - 0.2f) > 0.02f || kotlin.math.abs(pb - 0.1f) > 0.02f
        )

        val (dr, dg, db) = ColorMatrixBuilder.applyDaltonization(0.2f, 0.9f, 0.1f, ColorblindMode.DEUTERANOPIA, 1.0f)
        assertTrue(
            "Deuteranopia Daltonization must shift green contrast into red/blue",
            kotlin.math.abs(dr - 0.2f) > 0.02f || kotlin.math.abs(db - 0.1f) > 0.02f
        )

        val (tr, tg, tb) = ColorMatrixBuilder.applyDaltonization(0.1f, 0.2f, 0.9f, ColorblindMode.TRITANOPIA, 1.0f)
        assertTrue(
            "Tritanopia Daltonization must shift blue contrast into red/green",
            kotlin.math.abs(tr - 0.1f) > 0.02f || kotlin.math.abs(tg - 0.2f) > 0.02f
        )

        val dofStyle = GameFilterStyle(slot = 1, name = "Tilt-Shift", dofEnabled = true, dofNearBlur = 0.8f, dofFarBlur = 0.8f)
        assertTrue("Optical mode exposes API 31+ blur radius", computeOverlayBlurRadius(dofStyle, gpuShaderActive = false) > 10)
        assertEquals("GPU shader mode disables window blur behind to avoid double blur", 0, computeOverlayBlurRadius(dofStyle, gpuShaderActive = true))
    }

    @Test
    fun `GameFilterBridge updates live styles synchronously and GLSL shader covers all stages`() {
        GameFilterBridge.syncFromPersisted(GameFilterState.default())
        assertEquals(0, GameFilterBridge.liveFilterState.value.activeSlot)

        val vivid = GameFilterStyle(slot = 2, name = "Sharp", sharpenEnabled = true, sharpenAmount = 0.8f)
        GameFilterBridge.updateLiveStyle(vivid)
        assertEquals(2, GameFilterBridge.liveFilterState.value.activeSlot)
        assertEquals(0.8f, GameFilterBridge.liveFilterState.value.activeStyle?.sharpenAmount ?: 0f, 0.001f)

        val shader = FreestyleGlTextureView.FREESTYLE_FRAGMENT_BODY
        assertTrue(shader.contains("uSharpenParams"))
        assertTrue(shader.contains("uSharpenPlusParams"))
        assertTrue(shader.contains("uBcParams1"))
        assertTrue(shader.contains("uDetailsParams"))
        assertTrue(shader.contains("uColorParams"))
        assertTrue(shader.contains("uColorblindParams"))
        assertTrue(shader.contains("uNightModeInt"))
        assertTrue(shader.contains("uSplitParams"))
        assertTrue(shader.contains("uBwParams"))
        assertTrue(shader.contains("uSepiaInt"))
        assertTrue(shader.contains("uVignetteParams"))
        assertTrue(shader.contains("uDofParams"))
        assertTrue(shader.contains("uOldFilm1"))
        assertTrue(shader.contains("uMoodParams"))
        assertTrue(shader.contains("uSpecialFxParams"))
        assertTrue(shader.contains("uPainterlyParams"))
        assertTrue(shader.contains("uLetterboxParams"))
    }

    @Test
    fun `recording quality helper scales bitrate storage and hardware fallback lists accurately`() {
        val sd24Bitrate = RecordingConfigHelper.computeTargetBitrateBps(RecordingResolution.P480, RecordingFps.FPS_24)
        val hd30Bitrate = RecordingConfigHelper.computeTargetBitrateBps(RecordingResolution.P720, RecordingFps.FPS_30)
        val fhd60Bitrate = RecordingConfigHelper.computeTargetBitrateBps(RecordingResolution.P1080, RecordingFps.FPS_60)
        val native120Bitrate = RecordingConfigHelper.computeTargetBitrateBps(RecordingResolution.NATIVE, RecordingFps.FPS_120)

        assertTrue(sd24Bitrate < hd30Bitrate)
        assertTrue(hd30Bitrate < fhd60Bitrate)
        assertTrue(fhd60Bitrate < native120Bitrate)
        assertTrue(native120Bitrate <= 28_000_000)

        val hd30MbMin = RecordingConfigHelper.estimateStorageMbPerMinute(RecordingResolution.P720, RecordingFps.FPS_30)
        val fhd60MbMin = RecordingConfigHelper.estimateStorageMbPerMinute(RecordingResolution.P1080, RecordingFps.FPS_60)
        assertTrue(hd30MbMin in 25..40)
        assertTrue(fhd60MbMin > hd30MbMin)

        val resFallbacks = RecordingConfigHelper.fallbackLongEdgeCandidates(RecordingResolution.P1080)
        assertEquals(1920, resFallbacks.first())
        assertTrue(resFallbacks.zipWithNext().all { (a, b) -> a > b })
        assertTrue(resFallbacks.contains(1280))
        assertTrue(resFallbacks.contains(640))

        val fpsFallbacks = RecordingConfigHelper.fallbackFpsCandidates(RecordingFps.FPS_90)
        assertEquals(listOf(90, 60, 48, 30, 24), fpsFallbacks)
    }

    @Test
    fun `github update manager validates repo slugs download hosts semver and parses release json`() {
        assertEquals("octocat/NexusPad", GitHubUpdateManager.normalizeRepoSlug("octocat/NexusPad"))
        assertEquals("octocat/NexusPad", GitHubUpdateManager.normalizeRepoSlug("https://github.com/octocat/NexusPad/"))
        assertEquals("octocat/NexusPad", GitHubUpdateManager.normalizeRepoSlug("https://github.com/octocat/NexusPad.git"))
        assertEquals(null, GitHubUpdateManager.normalizeRepoSlug("not-a-repo"))
        assertEquals(null, GitHubUpdateManager.normalizeRepoSlug(""))

        assertTrue(GitHubUpdateManager.isAllowedGitHubDownloadUrl("https://github.com/octocat/NexusPad/releases/download/v1.0.5/NexusPad-v1.0.5.apk"))
        assertTrue(GitHubUpdateManager.isAllowedGitHubDownloadUrl("https://objects.githubusercontent.com/github-production-release-asset/123"))
        assertFalse(GitHubUpdateManager.isAllowedGitHubDownloadUrl("http://github.com/octocat/NexusPad/releases/download/v1.0.5/NexusPad.apk"))
        assertFalse(GitHubUpdateManager.isAllowedGitHubDownloadUrl("https://evil.example.com/NexusPad.apk"))

        assertTrue(GitHubUpdateManager.isNewerVersion("1.0", "v1.0.5"))
        assertTrue(GitHubUpdateManager.isNewerVersion("1.0.9", "v1.0.10"))
        assertTrue(GitHubUpdateManager.isNewerVersion("1.0.2", "1.1.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("1.0.10", "v1.0.10"))
        assertFalse(GitHubUpdateManager.isNewerVersion("1.0.11", "v1.0.10"))

        val sampleJson = """
            {
              "tag_name": "v1.0.12",
              "name": "NexusPad v1.0.12",
              "body": "### What's New\n- Recording 1080p 60 FPS\n- Direct in-app updates",
              "html_url": "https://github.com/octocat/NexusPad/releases/tag/v1.0.12",
              "published_at": "2026-11-23T12:00:00Z",
              "assets": [
                {
                  "name": "other.apk",
                  "browser_download_url": "https://github.com/octocat/NexusPad/releases/download/v1.0.12/other.apk",
                  "size": 1200000
                },
                {
                  "name": "NexusPad-v1.0.12.apk",
                  "browser_download_url": "https://github.com/octocat/NexusPad/releases/download/v1.0.12/NexusPad-v1.0.12.apk",
                  "size": 18450000
                }
              ]
            }
        """.trimIndent()

        val parsed = GitHubUpdateManager.parseReleaseJson(sampleJson)
        assertEquals("v1.0.12", parsed.tagName)
        assertEquals("1.0.12", parsed.versionName)
        assertEquals("NexusPad-v1.0.12.apk", parsed.apkFileName)
        assertEquals(18_450_000L, parsed.apkSizeBytes)
        assertTrue(parsed.apkDownloadUrl!!.endsWith("NexusPad-v1.0.12.apk"))
        assertTrue(parsed.releaseNotes.contains("Recording 1080p 60 FPS"))
    }

    @Test
    fun `app color scheme mixer resolves presets 2-color blend and 3-color custom accent`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(AppColorSchemeConfig::class.java)

        val defaultScheme = AppColorSchemeConfig()
        assertEquals(AppThemePreset.XENOS_DEFAULT, defaultScheme.preset)
        assertEquals(0xFF8E9CFFL, defaultScheme.effectivePrimaryHex())
        assertTrue(AppThemePreset.entries.contains(AppThemePreset.GEFORCE_LIME))
        assertEquals(0xFF8CE000L, AppThemePreset.GEFORCE_LIME.primaryHex)

        val preset = AppColorSchemeConfig(preset = AppThemePreset.CYBER_PINK)
        assertEquals(AppThemePreset.CYBER_PINK.primaryHex, preset.effectivePrimaryHex())
        assertEquals(AppThemePreset.CYBER_PINK.secondaryHex, preset.effectiveSecondaryHex())
        assertEquals(AppThemePreset.CYBER_PINK.tertiaryHex, preset.effectiveTertiaryHex())

        val mix2 = AppColorSchemeConfig(
            preset = AppThemePreset.CUSTOM_MIX,
            useThreeColors = false,
            customPrimaryHex = 0xFFFF0000,
            customSecondaryHex = 0xFF0000FF,
            customTertiaryHex = 0xFF00FF00
        )
        val blended = mix2.effectiveTertiaryHex()
        assertEquals(0xFF7F007FL, blended)

        val mix3 = mix2.copy(useThreeColors = true)
        assertEquals(0xFF00FF00L, mix3.effectiveTertiaryHex())

        val restored = adapter.fromJson(adapter.toJson(mix3))
        assertEquals(mix3, restored)
    }

    @Test
    fun `floating bubble config clamps size opacity and round-trips with font config`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val bubbleAdapter = moshi.adapter(FloatingBubbleConfig::class.java)
        val fontAdapter = moshi.adapter(AppFontConfig::class.java)

        val extremeSmall = FloatingBubbleConfig(sizeDp = 12f, opacity = 0.05f, style = FloatingBubbleStyle.HEX_BADGE)
        assertEquals(36f, extremeSmall.clampedSizeDp(), 0.001f)
        assertEquals(0.25f, extremeSmall.clampedOpacity(), 0.001f)

        val extremeLarge = FloatingBubbleConfig(sizeDp = 140f, opacity = 1.8f, style = FloatingBubbleStyle.CYBER_PILL)
        assertEquals(88f, extremeLarge.clampedSizeDp(), 0.001f)
        assertEquals(1.0f, extremeLarge.clampedOpacity(), 0.001f)

        assertEquals(extremeSmall, bubbleAdapter.fromJson(bubbleAdapter.toJson(extremeSmall)))

        val customFont = AppFontConfig(
            preset = AppFontPreset.CUSTOM_UPLOAD,
            customFontPath = "/data/user/0/com.aistudio.nexuspad.vktrmz/files/custom_fonts/arcade.ttf",
            customFontName = "arcade"
        )
        assertEquals(customFont, fontAdapter.fromJson(fontAdapter.toJson(customFont)))
    }

    @Test
    fun `keyboard word predictor auto-corrects typos predicts next words and supports Thai`() {
        val typo = KeyboardWordPredictor.predict("teh")
        assertEquals("the", typo.autoCorrect)
        assertEquals("the", typo.suggestions.first())

        val typoCap = KeyboardWordPredictor.predict("Recieve")
        assertEquals("Receive", typoCap.autoCorrect)

        val fuzzy = KeyboardWordPredictor.predict("playre")
        assertEquals("player", fuzzy.autoCorrect)

        val nextAfterGood = KeyboardWordPredictor.predict(currentWord = "", previousWord = "good", isThai = false)
        assertTrue(nextAfterGood.suggestions.isNotEmpty())
        assertTrue(nextAfterGood.suggestions.any { it.equals("game", ignoreCase = true) || it.equals("luck", ignoreCase = true) })

        val thaiPrefix = KeyboardWordPredictor.predict(currentWord = "สวัส", isThai = true)
        assertTrue("Expected Thai greeting suggestion", thaiPrefix.suggestions.contains("สวัสดี"))

        val thaiNext = KeyboardWordPredictor.predict(currentWord = "", previousWord = "ขอบคุณ", isThai = true)
        assertTrue(thaiNext.suggestions.isNotEmpty())
    }

    @Test
    fun `full app localization translates Freestyle Gallery Updates Personalization and WhatsNew across all 5 languages`() {
        val english = AppLocalization.stringsFor(AppLanguage.ENGLISH)
        val nonEnglish = listOf(AppLanguage.THAI, AppLanguage.SPANISH, AppLanguage.JAPANESE, AppLanguage.CHINESE)

        nonEnglish.forEach { lang ->
            val s = AppLocalization.stringsFor(lang)
            assertTrue("$lang filterPanelTitle blank", s.filterPanelTitle.isNotBlank())
            assertTrue("$lang galleryTitle blank", s.galleryTitle.isNotBlank())
            assertTrue("$lang recQualityTitle blank", s.recQualityTitle.isNotBlank())
            assertTrue("$lang updatesCardTitle blank", s.updatesCardTitle.isNotBlank())
            assertTrue("$lang themeCardTitle blank", s.themeCardTitle.isNotBlank())
            assertTrue("$lang fontCardTitle blank", s.fontCardTitle.isNotBlank())
            assertTrue("$lang bubbleCardTitle blank", s.bubbleCardTitle.isNotBlank())
            assertTrue("$lang whatsNewItems should have 5 entries", s.whatsNewItems.size == 5)
            assertFalse("$lang galleryTitle should be translated from English", s.galleryTitle == english.galleryTitle)
            assertFalse("$lang updatesCardTitle should be translated from English", s.updatesCardTitle == english.updatesCardTitle)
            assertFalse("$lang themeCardTitle should be translated from English", s.themeCardTitle == english.themeCardTitle)
        }
    }

    @Test
    fun `github update manager formats short signing fingerprint cleanly`() {
        val full = "C7:3F:97:29:AB:CD:EF:12:34:56:78:90:AA:BB:CC:DD"
        val short = GitHubUpdateManager.shortFingerprint(full)
        assertEquals("C73F97…BBCCDD", short)
        assertEquals("SHORT", GitHubUpdateManager.shortFingerprint("SHORT"))
        assertEquals("Unknown", GitHubUpdateManager.shortFingerprint(null))
    }

    @Test
    fun `cloud layout payload matches firestore security rules schema and round trips cleanly`() {
        val sample = PresetProfiles.pcMouseWasdPreset(id = "my custom/layout! 123").copy(
            globalOpacity = 0.05f, // out of bounds, should clamp to 0.2
            updatedAt = 1_700_000_000_000L
        )
        val safeId = CloudLayoutSyncRepository.sanitizeLayoutId(sample.id)
        assertTrue(Regex("^[a-zA-Z0-9_\\-]+$").matches(safeId))
        assertTrue(safeId.length in 1..128)

        val createPayload = CloudLayoutSyncRepository.toCreatePayload("user_abc_123", sample)
        val expectedCreateKeys = setOf(
            "userId", "name", "gameTitle", "genre", "globalOpacity",
            "gridSnap", "active", "controls", "clientUpdatedAt", "createdAt", "updatedAt"
        )
        assertEquals(expectedCreateKeys, createPayload.keys)
        assertFalse("Must never write an id field into document body", "id" in createPayload)
        assertEquals(0.2, createPayload["globalOpacity"] as Double, 0.0001)

        val updatePayload = CloudLayoutSyncRepository.toUpdatePayload(sample)
        val expectedUpdateKeys = setOf(
            "name", "gameTitle", "genre", "globalOpacity",
            "gridSnap", "active", "controls", "clientUpdatedAt", "updatedAt"
        )
        assertEquals(expectedUpdateKeys, updatePayload.keys)
        assertFalse("Update payload must never mutate userId", "userId" in updatePayload)
        assertFalse("Update payload must never mutate createdAt", "createdAt" in updatePayload)

        val restored = CloudLayoutSyncRepository.fromFirestoreDocumentMap(safeId, createPayload)
        assertNotNull(restored)
        assertEquals(safeId, restored?.id)
        assertEquals(sample.name, restored?.name)
        assertEquals(sample.genre, restored?.genre)
        assertEquals(sample.controls.size, restored?.controls?.size)
        assertEquals(0.2f, restored!!.globalOpacity, 0.001f)
        assertEquals(1_700_000_000_000L, restored.updatedAt)
    }

    @Test
    fun `cloud merge protects backed up layouts from freshly seeded presets after reinstall`() {
        val freshlySeeded = PresetProfiles.fpsPrecisionPreset(isActive = true)
            .copy(updatedAt = ProfileRepository.STARTER_SEED_TIMESTAMP)
        val cloudEditedPreset = freshlySeeded.copy(
            name = "My Custom Apex FPS",
            updatedAt = 1_700_000_500_000L,
            isActive = true
        )
        val cloudCustomLayout = PresetProfiles.createBlankProfile("DBD Survivor", "Dead by Daylight", GameGenre.ACTION_RPG)
            .copy(id = "custom_dbd_01", updatedAt = 1_700_000_600_000L)

        val plan = CloudLayoutSyncRepository.planMerge(
            localProfiles = listOf(freshlySeeded),
            cloudProfiles = listOf(cloudEditedPreset, cloudCustomLayout),
            forceCloudWins = false
        )

        assertEquals(2, plan.toSaveLocally.size)
        assertTrue(plan.toSaveLocally.any { it.name == "My Custom Apex FPS" })
        assertTrue(plan.toSaveLocally.any { it.id == "custom_dbd_01" })
        assertTrue("Freshly seeded starter preset should not overwrite cloud", plan.toPushToCloud.isEmpty())
        assertEquals(cloudEditedPreset.id, plan.preferredActiveProfileId)
    }

    @Test
    fun `cloud save UI strings are translated across all 5 languages`() {
        val english = AppLocalization.stringsFor(AppLanguage.ENGLISH)
        listOf(AppLanguage.THAI, AppLanguage.SPANISH, AppLanguage.JAPANESE, AppLanguage.CHINESE).forEach { lang ->
            val s = AppLocalization.stringsFor(lang)
            assertTrue("$lang cloudSaveTitle blank", s.cloudSaveTitle.isNotBlank())
            assertTrue("$lang cloudSaveSignInButton blank", s.cloudSaveSignInButton.isNotBlank())
            assertFalse("$lang cloudSaveTitle should be translated", s.cloudSaveTitle == english.cloudSaveTitle)
            assertFalse("$lang cloudSaveSignInButton should be translated", s.cloudSaveSignInButton == english.cloudSaveSignInButton)
        }
    }

    @Test
    fun `cloud appearance settings payload matches firestore rules schema and round trips cleanly`() {
        val snapshot = CloudAppearanceSnapshot(
            gameFilterJson = """{"activeSlot":1}""",
            crosshairJson = """{"enabled":true,"style":"DOT"}""",
            colorSchemeJson = """{"preset":"CYBER_PINK"}""",
            fontConfigJson = """{"preset":"ORBITRON"}""",
            floatingBubbleJson = """{"sizeDp":56.0,"opacity":0.85}""",
            darkTheme = true,
            clientUpdatedAt = 1_760_123_456_000L
        )

        val createPayload = CloudLayoutSyncRepository.toSettingsCreatePayload("uid_player_99", snapshot)
        val expectedCreateKeys = setOf(
            "userId", "gameFilterJson", "crosshairJson", "colorSchemeJson",
            "fontConfigJson", "floatingBubbleJson", "darkTheme",
            "clientUpdatedAt", "createdAt", "updatedAt"
        )
        assertEquals(expectedCreateKeys, createPayload.keys)
        assertFalse("Must never include id field", "id" in createPayload)
        assertEquals("uid_player_99", createPayload["userId"])

        val updatePayload = CloudLayoutSyncRepository.toSettingsUpdatePayload(snapshot)
        val expectedUpdateKeys = setOf(
            "gameFilterJson", "crosshairJson", "colorSchemeJson",
            "fontConfigJson", "floatingBubbleJson", "darkTheme",
            "clientUpdatedAt", "updatedAt"
        )
        assertEquals(expectedUpdateKeys, updatePayload.keys)
        assertFalse("Update payload must not mutate userId", "userId" in updatePayload)
        assertFalse("Update payload must not mutate createdAt", "createdAt" in updatePayload)

        val restored = CloudLayoutSyncRepository.fromSettingsDocumentMap(createPayload)
        assertNotNull(restored)
        assertEquals(snapshot.gameFilterJson, restored?.gameFilterJson)
        assertEquals(snapshot.crosshairJson, restored?.crosshairJson)
        assertEquals(snapshot.colorSchemeJson, restored?.colorSchemeJson)
        assertEquals(snapshot.fontConfigJson, restored?.fontConfigJson)
        assertEquals(snapshot.floatingBubbleJson, restored?.floatingBubbleJson)
        assertEquals(snapshot.darkTheme, restored?.darkTheme)
        assertEquals(snapshot.clientUpdatedAt, restored?.clientUpdatedAt)
    }

    @Test
    fun `six digit layout share codes validate normalize and match firestore rules schema`() {
        repeat(20) {
            val code = CloudLayoutSyncRepository.generateShareCode()
            assertEquals(6, code.length)
            assertTrue("Generated share code must match ^[A-Z0-9]{6}$", CloudLayoutSyncRepository.isValidShareCode(code))
        }

        assertEquals("AB39KX", CloudLayoutSyncRepository.normalizeShareCode(" ab3-9kx "))
        assertTrue(CloudLayoutSyncRepository.isValidShareCode("AB39KX"))
        assertFalse(CloudLayoutSyncRepository.isValidShareCode("AB39"))
        assertFalse(CloudLayoutSyncRepository.isValidShareCode("AB39KX99999999999"))
        assertFalse(CloudLayoutSyncRepository.isValidShareCode("AB39K!"))

        val profile = PresetProfiles.fpsPrecisionPreset(isActive = true)
        val nowMs = 1_760_200_000_000L
        val expiresMs = nowMs + 5 * 60 * 1000L
        val payload = CloudLayoutSyncRepository.toSharedLayoutCreatePayload(
            ownerUid = "uid_owner_7",
            authorName = "ProPlayer",
            profile = profile,
            expiresAtMs = expiresMs
        )
        val expectedKeys = setOf(
            "ownerId", "authorName", "name", "gameTitle", "genre",
            "globalOpacity", "gridSnap", "controls", "createdAt", "expiresAt"
        )
        assertEquals(expectedKeys, payload.keys)
        assertFalse("Shared layout must never include id in body", "id" in payload)

        val fetched = CloudLayoutSyncRepository.fromSharedLayoutDocumentMap("AB39KX", payload)
        assertNotNull(fetched)
        assertEquals("AB39KX", fetched?.shareCode)
        assertEquals("ProPlayer", fetched?.authorName)
        assertEquals(profile.name, fetched?.profile?.name)
        assertEquals(profile.controls.size, fetched?.profile?.controls?.size)
        assertFalse(fetched!!.profile.isActive)
    }

    @Test
    fun `gyroscope aim engine remaps display rotations applies deadzone invert and tracks ADS gating`() {
        val baseConfig = GyroAimConfig(
            enabled = true,
            sensitivity = 1.0f,
            deadzoneRadPerSec = 0.10f,
            smoothing = 0.25f,
            invertX = false,
            invertY = false,
            activationMode = GyroActivationMode.HOLD_ADS
        )

        // Small jitter below deadzone yields exact zero
        val (zeroDx, zeroDy) = GyroAimEngine.computeAimDeltaPx(
            gyroX = 0.04f,
            gyroY = -0.05f,
            gyroZ = 0.02f,
            dtSec = 0.016f,
            displayRotation = Surface.ROTATION_90,
            config = baseConfig
        )
        assertEquals(0f, zeroDx, 0.0001f)
        assertEquals(0f, zeroDy, 0.0001f)

        // Landscape left (ROTATION_90) vs Landscape right (ROTATION_270) invert axes accurately
        val (dx90, dy90) = GyroAimEngine.computeAimDeltaPx(
            gyroX = -1.0f,
            gyroY = 0.8f,
            gyroZ = 0f,
            dtSec = 0.016f,
            displayRotation = Surface.ROTATION_90,
            config = baseConfig
        )
        val (dx270, dy270) = GyroAimEngine.computeAimDeltaPx(
            gyroX = -1.0f,
            gyroY = 0.8f,
            gyroZ = 0f,
            dtSec = 0.016f,
            displayRotation = Surface.ROTATION_270,
            config = baseConfig
        )
        assertTrue(dx90 > 0f)
        assertTrue(dy90 > 0f)
        assertEquals(-dx90, dx270, 0.01f)
        assertEquals(-dy90, dy270, 0.01f)

        // Portrait (ROTATION_0) and Invert X / Y flags
        val (dxPort, dyPort) = GyroAimEngine.computeAimDeltaPx(
            gyroX = -1.0f,
            gyroY = -1.0f,
            gyroZ = 0f,
            dtSec = 0.016f,
            displayRotation = Surface.ROTATION_0,
            config = baseConfig
        )
        val (dxInv, dyInv) = GyroAimEngine.computeAimDeltaPx(
            gyroX = -1.0f,
            gyroY = -1.0f,
            gyroZ = 0f,
            dtSec = 0.016f,
            displayRotation = Surface.ROTATION_0,
            config = baseConfig.copy(invertX = true, invertY = true)
        )
        assertTrue(dxPort > 0f && dyPort > 0f)
        assertEquals(-dxPort, dxInv, 0.01f)
        assertEquals(-dyPort, dyInv, 0.01f)

        // Verify InputBridge ADS state tracks LT / RMB press & release
        assertFalse(InputBridge.isAdsActive.value)
        val rmb = CommandCatalog.findById("mouse_right")
        InputBridge.pressControl("ads_rmb", "RMB", rmb)
        assertTrue(InputBridge.isAdsActive.value)
        InputBridge.releaseControl("ads_rmb", "RMB", rmb)
        assertFalse(InputBridge.isAdsActive.value)

        // Moshi round-trip for GyroAimConfig and PerfHudConfig
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val gyroAdapter = moshi.adapter(GyroAimConfig::class.java)
        val perfAdapter = moshi.adapter(PerfHudConfig::class.java)
        assertEquals(baseConfig, gyroAdapter.fromJson(gyroAdapter.toJson(baseConfig)))
        val perf = PerfHudConfig(enabled = true, showFps = true, showBattery = true, showTemp = true, showRam = true, opacity = 0.9f)
        assertEquals(perf, perfAdapter.fromJson(perfAdapter.toJson(perf)))
    }

    @Test
    fun `performance monitor computes accurate 60fps and 120fps from frame timestamps`() {
        assertEquals(0, PerformanceMonitor.computeFpsFromFrameTimesNanos(emptyList()))
        assertEquals(0, PerformanceMonitor.computeFpsFromFrameTimesNanos(listOf(1_000_000_000L)))

        // 60 intervals spanning 1 second -> 60 FPS
        val frames60 = (0..60).map { i -> i * 16_666_667L }
        assertEquals(60, PerformanceMonitor.computeFpsFromFrameTimesNanos(frames60))

        // 120 intervals spanning 1 second -> 120 FPS
        val frames120 = (0..120).map { i -> i * 8_333_333L }
        assertEquals(120, PerformanceMonitor.computeFpsFromFrameTimesNanos(frames120))
    }

    @Test
    fun `new gyro perf hud share code and live edit strings are localized across all 5 languages`() {
        val english = AppLocalization.stringsFor(AppLanguage.ENGLISH)
        listOf(AppLanguage.THAI, AppLanguage.SPANISH, AppLanguage.JAPANESE, AppLanguage.CHINESE).forEach { lang ->
            val s = AppLocalization.stringsFor(lang)
            assertTrue("$lang gyroAimTitle blank", s.gyroAimTitle.isNotBlank())
            assertTrue("$lang perfHudTitle blank", s.perfHudTitle.isNotBlank())
            assertTrue("$lang shareCodeCreateTitle blank", s.shareCodeCreateTitle.isNotBlank())
            assertTrue("$lang liveEditToolbarTitle blank", s.liveEditToolbarTitle.isNotBlank())
            assertTrue("$lang liveEditTile blank", s.liveEditTile.isNotBlank())
            assertFalse("$lang gyroAimTitle should be translated", s.gyroAimTitle == english.gyroAimTitle)
            assertFalse("$lang perfHudTitle should be translated", s.perfHudTitle == english.perfHudTitle)
            assertFalse("$lang shareCodeCreateTitle should be translated", s.shareCodeCreateTitle == english.shareCodeCreateTitle)
            assertFalse("$lang liveEditTile should be translated", s.liveEditTile == english.liveEditTile)
        }
    }

    @Test
    fun `better gfn region bypass headers bootstrap js and config moshi round trip work`() {
        val offConfig = WebStreamQualityConfig()
        assertFalse(offConfig.regionPreset.isEnabled)
        assertTrue(BetterGfnScriptEngine.buildNavigationHeaders(offConfig).isEmpty())

        val jpConfig = WebStreamQualityConfig(
            regionPreset = GfnRegionPreset.JAPAN,
            userAgentMode = WebPlayerUserAgentMode.CHROME_OS,
            spoofTimezoneAndLocale = true,
            enableVirtualGamepad = true,
            stretchVideoToFill = true,
            enableCssFilters = true,
            showWebRtcStatsHud = true
        )
        val headers = BetterGfnScriptEngine.buildNavigationHeaders(jpConfig)
        assertEquals(GfnRegionPreset.JAPAN.fakeIp, headers["X-Forwarded-For"])
        assertEquals(GfnRegionPreset.JAPAN.acceptLanguage, headers["Accept-Language"])

        val bootstrap = BetterGfnScriptEngine.buildBootstrapScript(jpConfig)
        assertTrue(bootstrap.contains(GfnRegionPreset.JAPAN.fakeIp))
        assertTrue(bootstrap.contains("Asia/Tokyo"))
        assertTrue(bootstrap.contains("navigator.getGamepads"))
        assertTrue(bootstrap.contains("window.__xenosInput"))
        assertTrue(bootstrap.contains("RTCPeerConnection"))

        assertTrue(BetterGfnScriptEngine.isTrustedGfnOrigin("https://play.geforcenow.com/mall/"))
        assertTrue(BetterGfnScriptEngine.isTrustedGfnOrigin("https://login.nvidia.com/authorize"))
        assertFalse(BetterGfnScriptEngine.isTrustedGfnOrigin("http://play.geforcenow.com"))
        assertFalse(BetterGfnScriptEngine.isTrustedGfnOrigin("https://evil.example.com"))

        val nlConfig = WebStreamQualityConfig(regionPreset = GfnRegionPreset.NETHERLANDS)
        val nlHeaders = BetterGfnScriptEngine.buildNavigationHeaders(nlConfig)
        assertEquals(GfnRegionPreset.NETHERLANDS.fakeIp, nlHeaders["X-Forwarded-For"])
        assertEquals("Europe/Amsterdam", GfnRegionPreset.NETHERLANDS.timeZone)
        assertTrue(GfnRegionPreset.NETHERLANDS.hasFreeOneHourTier)

        val activeRegions = GfnRegionPreset.entries.filter { it.isEnabled }
        assertTrue("Must have active GFN regions", activeRegions.isNotEmpty())
        assertTrue("Every enabled VPN server must support Free 1h tier", activeRegions.all { it.hasFreeOneHourTier })
        assertTrue("Every enabled VPN server must have valid IP, language, and timezone", activeRegions.all {
            it.fakeIp.isNotBlank() && it.acceptLanguage.isNotBlank() && it.timeZone.isNotBlank()
        })

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(WebStreamQualityConfig::class.java)
        assertEquals(jpConfig, adapter.fromJson(adapter.toJson(jpConfig)))
    }

    @Test
    fun `better gfn script engine maps gamepad buttons dom keys css filters and webrtc stats`() {
        assertEquals(0, BetterGfnScriptEngine.mapKeyCodeToGamepadButtonIndex(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(1, BetterGfnScriptEngine.mapKeyCodeToGamepadButtonIndex(KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(2, BetterGfnScriptEngine.mapKeyCodeToGamepadButtonIndex(KeyEvent.KEYCODE_BUTTON_X))
        assertEquals(3, BetterGfnScriptEngine.mapKeyCodeToGamepadButtonIndex(KeyEvent.KEYCODE_BUTTON_Y))
        assertEquals(6, BetterGfnScriptEngine.mapKeyCodeToGamepadButtonIndex(KeyEvent.KEYCODE_BUTTON_L2))
        assertEquals(7, BetterGfnScriptEngine.mapKeyCodeToGamepadButtonIndex(KeyEvent.KEYCODE_BUTTON_R2))
        assertEquals(9, BetterGfnScriptEngine.mapKeyCodeToGamepadButtonIndex(KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(16, BetterGfnScriptEngine.mapKeyCodeToGamepadButtonIndex(KeyEvent.KEYCODE_BUTTON_MODE))
        assertEquals(null, BetterGfnScriptEngine.mapKeyCodeToGamepadButtonIndex(KeyEvent.KEYCODE_W))

        val keyW = BetterGfnScriptEngine.mapKeyCodeToDomKey(KeyEvent.KEYCODE_W)
        assertEquals("KeyW", keyW.code)
        assertEquals("w", keyW.key)
        assertEquals(87, keyW.keyCode)

        val keySpace = BetterGfnScriptEngine.mapKeyCodeToDomKey(KeyEvent.KEYCODE_SPACE)
        assertEquals("Space", keySpace.code)
        assertEquals(32, keySpace.keyCode)

        val keyShift = BetterGfnScriptEngine.mapKeyCodeToDomKey(KeyEvent.KEYCODE_SHIFT_LEFT)
        assertEquals("ShiftLeft", keyShift.code)
        assertEquals("Shift", keyShift.key)

        assertEquals("none", BetterGfnScriptEngine.buildCssFilterString(null))
        val styled = GameFilterStyle(
            slot = 1,
            name = "Vivid",
            brightnessContrastEnabled = true,
            exposure = 0.25f,
            contrast = 0.30f,
            colorEnabled = true,
            colorVibrance = 0.40f,
            temperature = 0.20f,
            sepiaEnabled = true,
            sepiaIntensity = 0.25f
        )
        val css = BetterGfnScriptEngine.buildCssFilterString(styled)
        assertTrue(css.contains("brightness("))
        assertTrue(css.contains("contrast("))
        assertTrue(css.contains("saturate("))
        assertTrue(css.contains("hue-rotate("))
        assertTrue(css.contains("sepia("))

        val videoJs = BetterGfnScriptEngine.buildVideoStyleScript(styled, stretchToFill = true, enableCssFilters = true)
        assertTrue(videoJs.contains("objectFit: 'fill'"))
        assertTrue(videoJs.contains("MutationObserver"))

        val statsJson = """{"fps":60,"rttMs":24,"bitrateMbps":32.5,"packetLossPercent":0.12,"jitterMs":1.4,"decodeMs":4.2,"resolution":"1920x1080","codec":"H264","connected":true}"""
        val stats = BetterGfnScriptEngine.parseWebRtcStatsJson(statsJson, nowMs = 1234L)
        assertEquals(60, stats.fps)
        assertEquals(24, stats.rttMs)
        assertEquals(32.5f, stats.bitrateMbps, 0.01f)
        assertEquals(0.12f, stats.packetLossPercent, 0.01f)
        assertEquals("1920x1080", stats.resolution)
        assertEquals("H264", stats.codec)
        assertTrue(stats.connected)
        assertEquals(1234L, stats.updatedAtMs)
    }

    @Test
    fun `InputBridge WebInputSink intercepts gamepad keyboard mouse stick and gyro inputs directly`() {
        val log = mutableListOf<String>()
        val sink = object : InputBridge.WebInputSink {
            override fun onGamepadButton(androidKeyCode: Int, isDown: Boolean) {
                log += "PAD:$androidKeyCode:$isDown"
            }

            override fun onKeyboardKey(androidKeyCode: Int, metaState: Int, isDown: Boolean) {
                log += "KEY:$androidKeyCode:$isDown"
            }

            override fun onMouseButton(button: Int, isDown: Boolean) {
                log += "MB:$button:$isDown"
            }

            override fun onMouseScroll(scrollUp: Boolean) {
                log += "WHEEL:$scrollUp"
            }

            override fun onMouseMove(dxPx: Float, dyPx: Float) {
                log += "MOVE:${dxPx.toInt()},${dyPx.toInt()}"
            }

            override fun onAnalogStick(isLeft: Boolean, x: Float, y: Float) {
                log += "STICK:$isLeft:${(x * 100).toInt()},${(y * 100).toInt()}"
            }

            override fun onResetAll() {
                log += "RESET"
            }
        }

        try {
            InputBridge.attachWebInputSink(sink)
            InputBridge.pressControl("a_btn", "A", CommandCatalog.findById("btn_a"))
            InputBridge.pressControl("w_key", "W", CommandCatalog.findById("key_w"))
            InputBridge.pressControl("lmb_btn", "LMB", CommandCatalog.findById("mouse_left"))
            InputBridge.updateAnalogStick(AxisPair.LEFT_STICK, "L-Stick", 0.8f, -0.5f, 0.1f, 1.0f, true)
            InputBridge.dispatchGyroAimDelta(12f, -6f)
            InputBridge.releaseAll()

            assertTrue(log.contains("PAD:${KeyEvent.KEYCODE_BUTTON_A}:true"))
            assertTrue(log.contains("KEY:${KeyEvent.KEYCODE_W}:true"))
            assertTrue(log.contains("MB:0:true"))
            assertTrue(log.any { it.startsWith("STICK:true:") })
            assertTrue(log.contains("MOVE:12,-6"))
            assertTrue(log.contains("RESET"))
        } finally {
            InputBridge.attachWebInputSink(null)
        }
    }

    @Test
    fun `web player and region bypass strings are localized across all 5 languages`() {
        val english = AppLocalization.stringsFor(AppLanguage.ENGLISH)
        listOf(AppLanguage.THAI, AppLanguage.SPANISH, AppLanguage.JAPANESE, AppLanguage.CHINESE).forEach { lang ->
            val s = AppLocalization.stringsFor(lang)
            assertTrue("$lang webPlayerTitle blank", s.webPlayerTitle.isNotBlank())
            assertTrue("$lang webPlayerLaunchButton blank", s.webPlayerLaunchButton.isNotBlank())
            assertTrue("$lang webPlayerRegionHeader blank", s.webPlayerRegionHeader.isNotBlank())
            assertTrue("$lang webPlayerRegionNote blank", s.webPlayerRegionNote.isNotBlank())
            assertFalse("$lang webPlayerLaunchButton should be translated", s.webPlayerLaunchButton == english.webPlayerLaunchButton)
            assertFalse("$lang webPlayerRegionHeader should be translated", s.webPlayerRegionHeader == english.webPlayerRegionHeader)
        }
    }

    @Test
    fun `better gfn web video filter generates valid svg color matrix and sharpen kernel`() {
        val neutral = GameFilterStyle(slot = 1, name = "Neutral")
        assertEquals(null, BetterGfnScriptEngine.buildSharpenKernelMatrix(neutral))

        val clarityStyle = GameFilterStyle(
            slot = 1,
            name = "Clarity+",
            sharpenEnabled = true,
            sharpenAmount = 0.6f,
            brightnessContrastEnabled = true,
            exposure = 0.2f,
            contrast = 0.25f,
            colorEnabled = true,
            colorVibrance = 0.4f,
            colorblindEnabled = true,
            colorblindMode = ColorblindMode.DEUTERANOPIA,
            colorblindIntensity = 0.5f,
            vignetteEnabled = true,
            vignetteIntensity = 0.4f,
            letterboxEnabled = true,
            letterboxHeight = 0.12f
        )

        val kernel = BetterGfnScriptEngine.buildSharpenKernelMatrix(clarityStyle)
        assertNotNull(kernel)
        val kernelNums = kernel!!.split(" ").map { it.toFloat() }
        assertEquals(9, kernelNums.size)
        assertEquals("Unsharp mask weights must sum to 1.0", 1.0f, kernelNums.sum(), 0.01f)

        val matrixStr = BetterGfnScriptEngine.buildSvgColorMatrixValues(clarityStyle)
        val matrixNums = matrixStr.split(" ").map { it.toFloat() }
        assertEquals(20, matrixNums.size)
        assertEquals("Alpha row must stay identity", 1.0f, matrixNums[18], 0.001f)

        val js = BetterGfnScriptEngine.buildVideoStyleScript(
            style = clarityStyle,
            stretchToFill = true,
            enableCssFilters = true
        )
        assertTrue(js.contains("xenos-gfn-video-filter"))
        assertTrue(js.contains("feConvolveMatrix"))
        assertTrue(js.contains("feColorMatrix"))
        assertTrue(js.contains("shadowRoot"))
        assertTrue(js.contains("xenos-gfn-fx-layer"))
    }

    @Test
    fun `web player trackpad analog routes to right stick and mouse mode avoids finger-lift double jump`() {
        val log = mutableListOf<String>()
        val sink = object : InputBridge.WebInputSink {
            override fun onGamepadButton(androidKeyCode: Int, isDown: Boolean) {
                log += "PAD:$androidKeyCode:$isDown"
            }

            override fun onKeyboardKey(androidKeyCode: Int, metaState: Int, isDown: Boolean) {
                log += "KEY:$androidKeyCode:$isDown"
            }

            override fun onMouseButton(button: Int, isDown: Boolean) {
                log += "MB:$button:$isDown"
            }

            override fun onMouseScroll(scrollUp: Boolean) {
                log += "WHEEL:$scrollUp"
            }

            override fun onMouseMove(dxPx: Float, dyPx: Float) {
                log += "MOVE:${dxPx.toInt()},${dyPx.toInt()}"
            }

            override fun onAnalogStick(isLeft: Boolean, x: Float, y: Float) {
                log += "STICK:$isLeft:${(x * 100).toInt()},${(y * 100).toInt()}"
            }

            override fun onResetAll() {
                log += "RESET"
            }
        }

        try {
            assertFalse(InputBridge.hasWebInputSink())
            InputBridge.attachWebInputSink(sink)
            assertTrue(InputBridge.hasWebInputSink())

            // 1. Trackpad Analog mode routes smoothly to Right Analog Stick (isLeft = false) with zero digital key mashing
            InputBridge.updateTouchpadLook(
                controlId = "pad_analog",
                sourceLabel = "TRACKPAD",
                axisPair = AxisPair.RIGHT_STICK,
                normX = 0.75f,
                normY = -0.50f,
                deadzone = 0.10f,
                invertY = false,
                active = true
            )
            assertTrue(log.any { it.startsWith("STICK:false:") })
            assertTrue("Analog trackpad in Web Player must not mash digital keys", log.none { it.startsWith("KEY:") })

            // 2. Trackpad Mouse mode streams live moves and skips redundant finger-lift flick swipe
            log.clear()
            InputBridge.updateTouchpadMouseMove(
                controlId = "pad_mouse",
                sourceLabel = "TRACKPAD",
                axisPair = AxisPair.TOUCHPAD_ARROWS,
                deltaXPx = 18f,
                deltaYPx = -9f,
                sensitivity = 1.0f,
                invertY = false,
                isFlickInertia = false,
                active = true
            )
            assertEquals(listOf("MOVE:18,-9"), log)

            log.clear()
            InputBridge.commitTouchpadFlickSwipe(
                sourceLabel = "TRACKPAD",
                totalDeltaXPx = 18f,
                totalDeltaYPx = -9f,
                sensitivity = 1.0f,
                invertY = false
            )
            assertTrue("commitTouchpadFlickSwipe must not double-jump Web Player mouse cursor", log.isEmpty())

            // 3. Two-finger right-click tap dispatches RMB down/up (button 2)
            InputBridge.dispatchMouseRightTapClick("TRACKPAD")
            assertEquals(listOf("MB:2:true", "MB:2:false"), log)

            // 4. BetterGfnScriptEngine emits full cursor sync + pointermove + elementFromPoint
            val bootstrap = BetterGfnScriptEngine.buildBootstrapScript(WebStreamQualityConfig())
            assertTrue(bootstrap.contains("elementFromPoint"))
            assertTrue(bootstrap.contains("pointermove"))
            assertTrue(bootstrap.contains("syncPointer"))
            val syncJs = BetterGfnScriptEngine.buildSyncMouseMoveJs(320f, 180f, 12f, -4f, forceRelative = false)
            assertTrue(syncJs.contains("syncPointer(320.00, 180.00, 12.00, -4.00, false)"))
        } finally {
            InputBridge.attachWebInputSink(null)
        }
    }
}
