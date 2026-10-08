package com.marie.thermalsystems.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Keybind for {@link ThermalSystemsHudClient}: translation key under {@code key.thermalsystems.*}, shared
 * {@code key.categories.thermalsystems} category, registered via {@link RegisterKeyMappingsEvent}
 * on the mod event bus.
 *
 * <p>Unbound by default ({@link InputConstants#UNKNOWN}) - unlike {@code EDIT_TOGGLE_POSITION},
 * which only affects a screen that's already open for another reason, this key can open an overlay
 * over ordinary gameplay at any time, so it shouldn't claim a key the player hasn't chosen for it.
 */
final class ThermalSystemsHudKeys {

    static final KeyMapping TOGGLE_PANEL = new KeyMapping(
            "key.thermalsystems.toggleControlPanel",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.thermalsystems"
    );

    /** Opens the Hub config screen in game; unbound by default for the same reason. */
    static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.thermalsystems.openConfig",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.thermalsystems"
    );

    private ThermalSystemsHudKeys() {
    }

    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_PANEL);
        event.register(OPEN_CONFIG);
    }
}
