package com.example

import android.view.KeyEvent
import com.example.input.InputBridge
import com.example.model.AppLanguage
import com.example.model.AppLocalization
import com.example.model.AxisPair
import com.example.model.ColorMatrixBuilder
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
import com.example.model.PresetProfiles
import com.example.model.TargetPlatformMode
import com.example.model.TouchpadMode
import com.example.ui.components.computeOverlayDimAmount
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
}
