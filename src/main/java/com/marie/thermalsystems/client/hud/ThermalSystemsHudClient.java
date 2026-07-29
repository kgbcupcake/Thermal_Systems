package com.marie.thermalsystems.client.hud;

import dev.marie.framework.ui.edit.EditModeController;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-only entry point for the persistent Thermal Systems control panel HUD - wires
 * {@link ThermalSystemsHudKeys#TOGGLE_PANEL} and opens {@link ThermalSystemsControlPanel} in a
 * {@link EditModeController} overlay when pressed. Called unconditionally from
 * {@code ThermalSystemsClient}'s constructor - unlike {@code EnderIOClientIntegration}, this isn't
 * gated behind any foreign mod's presence.
 *
 * <p>Minecraft has no on-screen mouse cursor during ordinary gameplay (no {@code Screen} open), so
 * there is no way to click or drag a HUD element without one - {@link EditModeController} is the
 * same mechanism Nourished's {@code NourishedHUD} uses to make its own HUD panel interactive,
 * opening a transparent, non-pausing {@code EditOverlayScreen} that forwards input to the target.
 * {@link ThermalSystemsHudKeys#TOGGLE_PANEL} therefore toggles the panel's visibility and its
 * interactivity together: pressing it opens the overlay (the panel becomes visible and clickable);
 * pressing it again, or Esc, closes it (handled by {@code EditOverlayScreen} itself, not polled
 * here - see {@link #onClientTick}).
 */
public final class ThermalSystemsHudClient {

    private static ThermalSystemsControlPanel panel;
    private static EditModeController editModeController;

    private ThermalSystemsHudClient() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(RegisterKeyMappingsEvent.class, ThermalSystemsHudKeys::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, ThermalSystemsHudClient::onClientTick);
    }

    /**
     * Only ever opens the overlay - never closes it. While the overlay is open, {@code mc.screen !=
     * null} bails out before {@link ThermalSystemsHudKeys#TOGGLE_PANEL} is even polled, matching
     * Nourished's {@code NourishedHUD#onClientTick} exactly: the same keypress that closes the
     * overlay is handled entirely by {@code EditOverlayScreen}'s own {@code keyPressed}, so this
     * method never needs (and must never attempt) to close it itself.
     */
    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        while (ThermalSystemsHudKeys.TOGGLE_PANEL.consumeClick()) {
            editModeController().enter();
        }
    }

    /**
     * Lazily built on first use - {@link ThermalSystemsControlPanel} and the controller wrapping it
     * have no natural earlier construction point (no screen of their own, unlike
     * {@code EnderIOClientIntegration#onScreenOpened}), so they live here as statics instead, same
     * as {@code NourishedHUD#marieEditModeController}.
     */
    private static EditModeController editModeController() {
        if (editModeController == null) {
            panel = new ThermalSystemsControlPanel();
            editModeController = new EditModeController(
                    panel,
                    "Thermal Systems control panel - drag to reposition. Press again or Esc to close.",
                    ThermalSystemsHudKeys.TOGGLE_PANEL.getKey().getValue(),
                    () -> {
                    });
        }
        return editModeController;
    }
}
