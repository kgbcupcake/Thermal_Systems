package com.marie.thermalsystems.integration.enderio;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Client keybinds for this integration - just {@link #EDIT_TOGGLE_POSITION} today, which toggles
 * whether {@link HeatCoolToggleComponent} can be dragged to a new spot on the Stirling Generator
 * screen. Same {@code KeyMapping}-per-editable-target convention Nourished's
 * {@code NourishedKeys}/{@code EDIT_HUD}/{@code EDIT_DIET_SCREEN} already use for entering a
 * marie-ui edit mode, rather than inventing a new interaction (a settings toggle, a right-click,
 * a double-click) for just this one widget.
 */
final class EnderIOKeys {

    static final KeyMapping EDIT_TOGGLE_POSITION = new KeyMapping(
            "key.thermalsystems.editEnderIOTogglePosition",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.thermalsystems"
    );

    private EnderIOKeys() {
    }

    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(EDIT_TOGGLE_POSITION);
    }
}
