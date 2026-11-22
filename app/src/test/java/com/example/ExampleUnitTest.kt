package com.example

import android.view.KeyEvent
import com.example.input.InputBridge
import com.example.model.AxisPair
import com.example.model.CommandCatalog
import com.example.model.ControllerNotation
import com.example.model.ControllerProfile
import com.example.model.GameGenre
import com.example.model.PresetProfiles
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExampleUnitTest {

    @Before
    fun setup() {
        InputBridge.releaseAll()
        InputBridge.clearTelemetry()
        InputBridge.attachImeSink(null)
    }

    @Test
    fun `starter presets contain all four GeForce NOW layouts`() {
        val presets = PresetProfiles.createStarterProfiles()
        assertEquals(4, presets.size)
        assertTrue(presets.first().isActive)
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
}
