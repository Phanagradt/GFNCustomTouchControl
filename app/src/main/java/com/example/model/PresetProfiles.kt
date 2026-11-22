package com.example.model

import java.util.UUID

object PresetProfiles {

    fun createStarterProfiles(): List<ControllerProfile> = listOf(
        fpsPrecisionPreset(isActive = true),
        actionRpgPreset(),
        hybridWasdMmoPreset(),
        racingTelemetryPreset()
    )

    fun fpsPrecisionPreset(id: String = "preset_fps_precision", isActive: Boolean = false): ControllerProfile =
        ControllerProfile(
            id = id,
            name = "GeForce FPS Precision",
            gameTitle = "Apex Legends / Destiny 2 / CoD",
            genre = GameGenre.FPS,
            isActive = isActive,
            controls = listOf(
                VirtualControl(
                    id = "fps_lstick",
                    label = "Move",
                    controlType = ControlType.ANALOG_STICK,
                    shape = ControlShape.RING,
                    xPercent = 0.16f,
                    yPercent = 0.68f,
                    sizeDp = 128f,
                    opacity = 0.80f,
                    accent = ControlAccent.LIME,
                    deadzone = 0.10f,
                    sensitivity = 1.15f,
                    binding = CommandCatalog.findById("axis_left")
                ),
                VirtualControl(
                    id = "fps_rstick",
                    label = "Aim",
                    controlType = ControlType.ANALOG_STICK,
                    shape = ControlShape.RING,
                    xPercent = 0.78f,
                    yPercent = 0.66f,
                    sizeDp = 124f,
                    opacity = 0.78f,
                    accent = ControlAccent.CYAN,
                    deadzone = 0.08f,
                    sensitivity = 1.40f,
                    binding = CommandCatalog.findById("axis_right")
                ),
                VirtualControl(
                    id = "fps_rt",
                    label = "FIRE",
                    controlType = ControlType.SHOULDER_PADDLE,
                    shape = ControlShape.PADDLE,
                    xPercent = 0.87f,
                    yPercent = 0.24f,
                    sizeDp = 74f,
                    opacity = 0.88f,
                    accent = ControlAccent.CORAL,
                    behavior = TriggerBehavior.HOLD,
                    binding = CommandCatalog.findById("btn_rt")
                ),
                VirtualControl(
                    id = "fps_lt",
                    label = "AIM",
                    controlType = ControlType.SHOULDER_PADDLE,
                    shape = ControlShape.PADDLE,
                    xPercent = 0.13f,
                    yPercent = 0.24f,
                    sizeDp = 74f,
                    opacity = 0.86f,
                    accent = ControlAccent.CYAN,
                    behavior = TriggerBehavior.TOGGLE,
                    binding = CommandCatalog.findById("btn_lt")
                ),
                VirtualControl(
                    id = "fps_rb",
                    label = "RB",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.PILL,
                    xPercent = 0.75f,
                    yPercent = 0.22f,
                    sizeDp = 56f,
                    accent = ControlAccent.LIME,
                    binding = CommandCatalog.findById("btn_rb")
                ),
                VirtualControl(
                    id = "fps_lb",
                    label = "LB",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.PILL,
                    xPercent = 0.25f,
                    yPercent = 0.22f,
                    sizeDp = 56f,
                    accent = ControlAccent.LIME,
                    binding = CommandCatalog.findById("btn_lb")
                ),
                VirtualControl(
                    id = "fps_a",
                    label = "A",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.CIRCLE,
                    xPercent = 0.90f,
                    yPercent = 0.78f,
                    sizeDp = 60f,
                    accent = ControlAccent.LIME,
                    binding = CommandCatalog.findById("btn_a")
                ),
                VirtualControl(
                    id = "fps_b",
                    label = "B",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.CIRCLE,
                    xPercent = 0.92f,
                    yPercent = 0.56f,
                    sizeDp = 56f,
                    accent = ControlAccent.CORAL,
                    binding = CommandCatalog.findById("btn_b")
                ),
                VirtualControl(
                    id = "fps_x",
                    label = "R / X",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.CIRCLE,
                    xPercent = 0.65f,
                    yPercent = 0.80f,
                    sizeDp = 56f,
                    accent = ControlAccent.CYAN,
                    binding = CommandCatalog.findById("btn_x")
                ),
                VirtualControl(
                    id = "fps_y",
                    label = "Y",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.CIRCLE,
                    xPercent = 0.88f,
                    yPercent = 0.40f,
                    sizeDp = 54f,
                    accent = ControlAccent.AMBER,
                    binding = CommandCatalog.findById("btn_y")
                ),
                VirtualControl(
                    id = "fps_tab",
                    label = "TAB",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.HEX,
                    xPercent = 0.42f,
                    yPercent = 0.16f,
                    sizeDp = 52f,
                    accent = ControlAccent.CYAN,
                    binding = CommandCatalog.findById("key_tab")
                ),
                VirtualControl(
                    id = "fps_gfn",
                    label = "GFN",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.PILL,
                    xPercent = 0.58f,
                    yPercent = 0.16f,
                    sizeDp = 54f,
                    accent = ControlAccent.VIOLET,
                    binding = CommandCatalog.findById("combo_gfn_hud")
                )
            )
        )

    fun actionRpgPreset(id: String = "preset_action_rpg"): ControllerProfile =
        ControllerProfile(
            id = id,
            name = "Action RPG + Hotbar",
            gameTitle = "Cyberpunk 2077 / Elden Ring",
            genre = GameGenre.ACTION_RPG,
            controls = listOf(
                VirtualControl(
                    id = "rpg_lstick",
                    label = "Move",
                    controlType = ControlType.ANALOG_STICK,
                    shape = ControlShape.RING,
                    xPercent = 0.16f,
                    yPercent = 0.70f,
                    sizeDp = 126f,
                    accent = ControlAccent.LIME,
                    binding = CommandCatalog.findById("axis_left")
                ),
                VirtualControl(
                    id = "rpg_dpad",
                    label = "Quick",
                    controlType = ControlType.DPAD_CLUSTER,
                    shape = ControlShape.CROSS,
                    xPercent = 0.30f,
                    yPercent = 0.38f,
                    sizeDp = 104f,
                    accent = ControlAccent.SLATE,
                    binding = CommandCatalog.findById("cluster_dpad")
                ),
                VirtualControl(
                    id = "rpg_a",
                    label = "A",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.CIRCLE,
                    xPercent = 0.84f,
                    yPercent = 0.78f,
                    sizeDp = 58f,
                    accent = ControlAccent.LIME,
                    binding = CommandCatalog.findById("btn_a")
                ),
                VirtualControl(
                    id = "rpg_b",
                    label = "Dodge",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.CIRCLE,
                    xPercent = 0.91f,
                    yPercent = 0.64f,
                    sizeDp = 58f,
                    accent = ControlAccent.CORAL,
                    binding = CommandCatalog.findById("btn_b")
                ),
                VirtualControl(
                    id = "rpg_x",
                    label = "X",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.CIRCLE,
                    xPercent = 0.77f,
                    yPercent = 0.64f,
                    sizeDp = 56f,
                    accent = ControlAccent.CYAN,
                    binding = CommandCatalog.findById("btn_x")
                ),
                VirtualControl(
                    id = "rpg_y",
                    label = "Y",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.CIRCLE,
                    xPercent = 0.84f,
                    yPercent = 0.50f,
                    sizeDp = 56f,
                    accent = ControlAccent.AMBER,
                    binding = CommandCatalog.findById("btn_y")
                ),
                VirtualControl(
                    id = "rpg_rb",
                    label = "Attack",
                    controlType = ControlType.SHOULDER_PADDLE,
                    shape = ControlShape.PADDLE,
                    xPercent = 0.86f,
                    yPercent = 0.24f,
                    sizeDp = 70f,
                    accent = ControlAccent.LIME,
                    binding = CommandCatalog.findById("btn_rb")
                ),
                VirtualControl(
                    id = "rpg_lb",
                    label = "Guard",
                    controlType = ControlType.SHOULDER_PADDLE,
                    shape = ControlShape.PADDLE,
                    xPercent = 0.14f,
                    yPercent = 0.24f,
                    sizeDp = 70f,
                    accent = ControlAccent.CYAN,
                    binding = CommandCatalog.findById("btn_lb")
                ),
                VirtualControl(
                    id = "rpg_map",
                    label = "MAP",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.PILL,
                    xPercent = 0.44f,
                    yPercent = 0.16f,
                    sizeDp = 52f,
                    accent = ControlAccent.CYAN,
                    binding = CommandCatalog.findById("key_m")
                ),
                VirtualControl(
                    id = "rpg_inv",
                    label = "INV",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.PILL,
                    xPercent = 0.56f,
                    yPercent = 0.16f,
                    sizeDp = 52f,
                    accent = ControlAccent.CYAN,
                    binding = CommandCatalog.findById("key_i")
                ),
                VirtualControl(
                    id = "rpg_f5",
                    label = "F5",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.HEX,
                    xPercent = 0.68f,
                    yPercent = 0.16f,
                    sizeDp = 50f,
                    accent = ControlAccent.AMBER,
                    binding = CommandCatalog.findById("key_f5")
                )
            )
        )

    fun hybridWasdMmoPreset(id: String = "preset_wasd_mmo"): ControllerProfile =
        ControllerProfile(
            id = id,
            name = "Hybrid WASD + Hotkeys",
            gameTitle = "PC Strategy / MMO Cloud",
            genre = GameGenre.MMO_MOBA,
            controls = listOf(
                VirtualControl(
                    id = "mmo_wasd",
                    label = "WASD",
                    controlType = ControlType.WASD_CLUSTER,
                    shape = ControlShape.CROSS,
                    xPercent = 0.17f,
                    yPercent = 0.68f,
                    sizeDp = 128f,
                    accent = ControlAccent.CYAN,
                    binding = CommandCatalog.findById("cluster_wasd")
                ),
                VirtualControl(
                    id = "mmo_shift",
                    label = "SHIFT",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.PILL,
                    xPercent = 0.12f,
                    yPercent = 0.30f,
                    sizeDp = 58f,
                    accent = ControlAccent.AMBER,
                    behavior = TriggerBehavior.TOGGLE,
                    binding = CommandCatalog.findById("key_shift")
                ),
                VirtualControl(
                    id = "mmo_space",
                    label = "SPACE",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.PILL,
                    xPercent = 0.86f,
                    yPercent = 0.78f,
                    sizeDp = 70f,
                    accent = ControlAccent.LIME,
                    binding = CommandCatalog.findById("key_space")
                ),
                VirtualControl(
                    id = "mmo_1",
                    label = "1",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.HEX,
                    xPercent = 0.68f,
                    yPercent = 0.76f,
                    sizeDp = 54f,
                    accent = ControlAccent.CYAN,
                    binding = CommandCatalog.findById("key_1")
                ),
                VirtualControl(
                    id = "mmo_2",
                    label = "2",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.HEX,
                    xPercent = 0.76f,
                    yPercent = 0.60f,
                    sizeDp = 54f,
                    accent = ControlAccent.CYAN,
                    binding = CommandCatalog.findById("key_2")
                ),
                VirtualControl(
                    id = "mmo_3",
                    label = "3",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.HEX,
                    xPercent = 0.87f,
                    yPercent = 0.56f,
                    sizeDp = 54f,
                    accent = ControlAccent.CYAN,
                    binding = CommandCatalog.findById("key_3")
                ),
                VirtualControl(
                    id = "mmo_4",
                    label = "4",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.HEX,
                    xPercent = 0.88f,
                    yPercent = 0.34f,
                    sizeDp = 54f,
                    accent = ControlAccent.VIOLET,
                    behavior = TriggerBehavior.TURBO,
                    turboRateHz = 8,
                    binding = CommandCatalog.findById("key_4")
                ),
                VirtualControl(
                    id = "mmo_esc",
                    label = "ESC",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.PILL,
                    xPercent = 0.50f,
                    yPercent = 0.16f,
                    sizeDp = 52f,
                    accent = ControlAccent.SLATE,
                    binding = CommandCatalog.findById("key_esc")
                )
            )
        )

    fun racingTelemetryPreset(id: String = "preset_racing_sim"): ControllerProfile =
        ControllerProfile(
            id = id,
            name = "Racing Telemetry Pad",
            gameTitle = "Forza / Rocket League",
            genre = GameGenre.RACING,
            controls = listOf(
                VirtualControl(
                    id = "race_steer",
                    label = "Steer",
                    controlType = ControlType.ANALOG_STICK,
                    shape = ControlShape.RING,
                    xPercent = 0.18f,
                    yPercent = 0.66f,
                    sizeDp = 136f,
                    accent = ControlAccent.LIME,
                    deadzone = 0.05f,
                    sensitivity = 1.1f,
                    binding = CommandCatalog.findById("axis_left")
                ),
                VirtualControl(
                    id = "race_gas",
                    label = "GAS (RT)",
                    controlType = ControlType.SHOULDER_PADDLE,
                    shape = ControlShape.PADDLE,
                    xPercent = 0.87f,
                    yPercent = 0.68f,
                    sizeDp = 82f,
                    accent = ControlAccent.LIME,
                    binding = CommandCatalog.findById("btn_rt")
                ),
                VirtualControl(
                    id = "race_brake",
                    label = "BRAKE",
                    controlType = ControlType.SHOULDER_PADDLE,
                    shape = ControlShape.PADDLE,
                    xPercent = 0.72f,
                    yPercent = 0.74f,
                    sizeDp = 74f,
                    accent = ControlAccent.CORAL,
                    binding = CommandCatalog.findById("btn_lt")
                ),
                VirtualControl(
                    id = "race_shift_up",
                    label = "SHIFT +",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.PILL,
                    xPercent = 0.86f,
                    yPercent = 0.34f,
                    sizeDp = 62f,
                    accent = ControlAccent.CYAN,
                    binding = CommandCatalog.findById("btn_rb")
                ),
                VirtualControl(
                    id = "race_shift_down",
                    label = "SHIFT -",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.PILL,
                    xPercent = 0.14f,
                    yPercent = 0.28f,
                    sizeDp = 62f,
                    accent = ControlAccent.CYAN,
                    binding = CommandCatalog.findById("btn_lb")
                ),
                VirtualControl(
                    id = "race_handbrake",
                    label = "DRIFT",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.CIRCLE,
                    xPercent = 0.73f,
                    yPercent = 0.48f,
                    sizeDp = 58f,
                    accent = ControlAccent.AMBER,
                    binding = CommandCatalog.findById("btn_a")
                )
            )
        )

    fun createBlankProfile(name: String, gameTitle: String, genre: GameGenre): ControllerProfile =
        ControllerProfile(
            id = UUID.randomUUID().toString(),
            name = name,
            gameTitle = gameTitle,
            genre = genre,
            controls = listOf(
                VirtualControl(
                    id = UUID.randomUUID().toString(),
                    label = "Move",
                    controlType = ControlType.ANALOG_STICK,
                    shape = ControlShape.RING,
                    xPercent = 0.18f,
                    yPercent = 0.68f,
                    sizeDp = 124f,
                    accent = ControlAccent.LIME,
                    binding = CommandCatalog.findById("axis_left")
                ),
                VirtualControl(
                    id = UUID.randomUUID().toString(),
                    label = "A",
                    controlType = ControlType.BUTTON,
                    shape = ControlShape.CIRCLE,
                    xPercent = 0.84f,
                    yPercent = 0.72f,
                    sizeDp = 62f,
                    accent = ControlAccent.LIME,
                    binding = CommandCatalog.findById("btn_a")
                )
            )
        )
}
