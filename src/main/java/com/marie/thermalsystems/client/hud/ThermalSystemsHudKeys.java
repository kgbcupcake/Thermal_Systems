package com.marie.thermalsystems.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Keybind for {@link ThermalSystemsHudClient} - separate from
 * {@code com.marie.thermalsystems.integration.enderio.EnderIOKeys#EDIT_TOGGLE_POSITION}, which
 * only ever matters while a Stirling Generator's own screen is open. Same naming/registration
 * convention as {@code EnderIOKeys}: translation key under {@code key.thermalsystems.*}, shared
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

    private ThermalSystemsHudKeys() {
    }

    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_PANEL);
    }
}
