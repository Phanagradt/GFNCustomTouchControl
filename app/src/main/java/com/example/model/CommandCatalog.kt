package com.example.model

import android.view.KeyEvent

object CommandCatalog {

    val controllerCommands: List<InputBinding> = listOf(
        InputBinding("btn_a", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_BUTTON_A, codeLabel = "A", commandTitle = "Gamepad A (Confirm / Jump)"),
        InputBinding("btn_b", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_BUTTON_B, codeLabel = "B", commandTitle = "Gamepad B (Cancel / Crouch)"),
        InputBinding("btn_x", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_BUTTON_X, codeLabel = "X", commandTitle = "Gamepad X (Reload / Interact)"),
        InputBinding("btn_y", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_BUTTON_Y, codeLabel = "Y", commandTitle = "Gamepad Y (Switch Weapon)"),
        InputBinding("btn_lb", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_BUTTON_L1, codeLabel = "LB", commandTitle = "Left Bumper (L1)"),
        InputBinding("btn_rb", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_BUTTON_R1, codeLabel = "RB", commandTitle = "Right Bumper (R1)"),
        InputBinding("btn_lt", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_BUTTON_L2, codeLabel = "LT", commandTitle = "Left Trigger (L2 / Aim)"),
        InputBinding("btn_rt", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_BUTTON_R2, codeLabel = "RT", commandTitle = "Right Trigger (R2 / Fire)"),
        InputBinding("btn_l3", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_BUTTON_THUMBL, codeLabel = "L3", commandTitle = "Left Stick Click (Sprint)"),
        InputBinding("btn_r3", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_BUTTON_THUMBR, codeLabel = "R3", commandTitle = "Right Stick Click (Melee)"),
        InputBinding("btn_start", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_BUTTON_START, codeLabel = "MENU", commandTitle = "Start / Menu Button"),
        InputBinding("btn_select", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_BUTTON_SELECT, codeLabel = "VIEW", commandTitle = "View / Back Button"),
        InputBinding("btn_mode", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_BUTTON_MODE, codeLabel = "GFN", commandTitle = "Guide / GeForce Overlay"),
        InputBinding("dpad_up", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_DPAD_UP, codeLabel = "UP", commandTitle = "D-Pad Up"),
        InputBinding("dpad_down", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_DPAD_DOWN, codeLabel = "DN", commandTitle = "D-Pad Down"),
        InputBinding("dpad_left", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_DPAD_LEFT, codeLabel = "LT", commandTitle = "D-Pad Left"),
        InputBinding("dpad_right", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_DPAD_RIGHT, codeLabel = "RT", commandTitle = "D-Pad Right")
    )

    val analogCommands: List<InputBinding> = listOf(
        InputBinding("axis_left", BindingDomain.ANALOG_AXIS, KeyEvent.KEYCODE_UNKNOWN, codeLabel = "L-STICK", commandTitle = "Left Analog Stick (Move)", axisPair = AxisPair.LEFT_STICK),
        InputBinding("axis_right", BindingDomain.ANALOG_AXIS, KeyEvent.KEYCODE_UNKNOWN, codeLabel = "R-STICK", commandTitle = "Right Analog Stick (Camera)", axisPair = AxisPair.RIGHT_STICK),
        InputBinding("cluster_dpad", BindingDomain.CONTROLLER, KeyEvent.KEYCODE_DPAD_CENTER, codeLabel = "D-PAD", commandTitle = "4-Way Gamepad D-Pad", axisPair = AxisPair.DPAD_4WAY),
        InputBinding("cluster_wasd", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_W, codeLabel = "WASD", commandTitle = "WASD Directional Cluster", axisPair = AxisPair.WASD_KEYS),
        InputBinding("cluster_arrows", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_DPAD_UP, codeLabel = "ARROWS", commandTitle = "Arrow Keys Cluster", axisPair = AxisPair.ARROW_KEYS)
    )

    val keyboardCommands: List<InputBinding> = listOf(
        InputBinding("key_space", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_SPACE, codeLabel = "SPACE", commandTitle = "Spacebar (Jump)"),
        InputBinding("key_shift", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_SHIFT_LEFT, modifierMetaState = KeyEvent.META_SHIFT_ON, codeLabel = "SHIFT", commandTitle = "Left Shift (Sprint)"),
        InputBinding("key_ctrl", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_CTRL_LEFT, modifierMetaState = KeyEvent.META_CTRL_ON, codeLabel = "CTRL", commandTitle = "Left Ctrl (Crouch)"),
        InputBinding("key_alt", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_ALT_LEFT, modifierMetaState = KeyEvent.META_ALT_ON, codeLabel = "ALT", commandTitle = "Left Alt (Walk / Ping)"),
        InputBinding("key_tab", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_TAB, codeLabel = "TAB", commandTitle = "Tab (Inventory / Scoreboard)"),
        InputBinding("key_esc", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_ESCAPE, codeLabel = "ESC", commandTitle = "Escape (Pause Menu)"),
        InputBinding("key_enter", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_ENTER, codeLabel = "ENTER", commandTitle = "Enter / Chat"),
        InputBinding("key_w", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_W, codeLabel = "W", commandTitle = "Key W (Forward)"),
        InputBinding("key_a", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_A, codeLabel = "A", commandTitle = "Key A (Strafe Left)"),
        InputBinding("key_s", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_S, codeLabel = "S", commandTitle = "Key S (Backward)"),
        InputBinding("key_d", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_D, codeLabel = "D", commandTitle = "Key D (Strafe Right)"),
        InputBinding("key_e", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_E, codeLabel = "E", commandTitle = "Key E (Interact / Use)"),
        InputBinding("key_r", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_R, codeLabel = "R", commandTitle = "Key R (Reload)"),
        InputBinding("key_f", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_F, codeLabel = "F", commandTitle = "Key F (Melee / Action)"),
        InputBinding("key_q", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_Q, codeLabel = "Q", commandTitle = "Key Q (Ability 1)"),
        InputBinding("key_g", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_G, codeLabel = "G", commandTitle = "Key G (Grenade)"),
        InputBinding("key_c", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_C, codeLabel = "C", commandTitle = "Key C (Crouch)"),
        InputBinding("key_v", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_V, codeLabel = "V", commandTitle = "Key V (Voice / Melee)"),
        InputBinding("key_m", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_M, codeLabel = "M", commandTitle = "Key M (World Map)"),
        InputBinding("key_i", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_I, codeLabel = "I", commandTitle = "Key I (Inventory)"),
        InputBinding("key_1", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_1, codeLabel = "1", commandTitle = "Hotbar 1"),
        InputBinding("key_2", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_2, codeLabel = "2", commandTitle = "Hotbar 2"),
        InputBinding("key_3", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_3, codeLabel = "3", commandTitle = "Hotbar 3"),
        InputBinding("key_4", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_4, codeLabel = "4", commandTitle = "Hotbar 4"),
        InputBinding("key_5", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_5, codeLabel = "5", commandTitle = "Hotbar 5"),
        InputBinding("key_6", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_6, codeLabel = "6", commandTitle = "Hotbar 6"),
        InputBinding("key_f1", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_F1, codeLabel = "F1", commandTitle = "Function F1"),
        InputBinding("key_f2", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_F2, codeLabel = "F2", commandTitle = "Function F2"),
        InputBinding("key_f5", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_F5, codeLabel = "F5", commandTitle = "Quick Save (F5)"),
        InputBinding("key_f9", BindingDomain.KEYBOARD, KeyEvent.KEYCODE_F9, codeLabel = "F9", commandTitle = "Quick Load (F9)")
    )

    val comboCommands: List<InputBinding> = listOf(
        InputBinding("combo_gfn_hud", BindingDomain.COMBO, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_ON, "CTRL+G", "GeForce NOW Overlay (Ctrl+G)"),
        InputBinding("combo_steam", BindingDomain.COMBO, KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_ON, "SHF+TAB", "Steam Overlay (Shift+Tab)"),
        InputBinding("combo_ult", BindingDomain.COMBO, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, 0, "LB+RB", "Ultimate Ability (LB + RB)"),
        InputBinding("combo_sprint_jump", BindingDomain.COMBO, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_ON, "SHF+SPC", "Sprint Jump Chord"),
        InputBinding("combo_ctrl_r", BindingDomain.COMBO, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_ON, "CTRL+R", "Tactical Reset (Ctrl+R)")
    )

    val allCommands: List<InputBinding> =
        controllerCommands + analogCommands + keyboardCommands + comboCommands

    fun findById(id: String): InputBinding =
        allCommands.firstOrNull { it.id == id } ?: controllerCommands.first()

    fun formatFaceLabel(binding: InputBinding, notation: ControllerNotation): String {
        if (binding.domain != BindingDomain.CONTROLLER) return binding.codeLabel
        return when (notation) {
            ControllerNotation.XBOX -> binding.codeLabel
            ControllerNotation.PLAYSTATION -> when (binding.primaryKeyCode) {
                KeyEvent.KEYCODE_BUTTON_A -> "✕"
                KeyEvent.KEYCODE_BUTTON_B -> "○"
                KeyEvent.KEYCODE_BUTTON_X -> "□"
                KeyEvent.KEYCODE_BUTTON_Y -> "△"
                KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
                KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
                KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
                KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
                else -> binding.codeLabel
            }
            ControllerNotation.PC_HYBRID -> when (binding.primaryKeyCode) {
                KeyEvent.KEYCODE_BUTTON_A -> "BTN_A"
                KeyEvent.KEYCODE_BUTTON_B -> "BTN_B"
                KeyEvent.KEYCODE_BUTTON_X -> "BTN_X"
                KeyEvent.KEYCODE_BUTTON_Y -> "BTN_Y"
                else -> binding.codeLabel
            }
        }
    }
}
