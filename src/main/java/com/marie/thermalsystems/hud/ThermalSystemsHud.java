package com.marie.thermalsystems.hud;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common/server-side half of the persistent Thermal Systems control panel HUD - registers
 * {@link SystemToggleRequestPayload} and applies it to {@link ThermalConfig#SYSTEM_ENABLED}. The
 * client-only half (the panel component, its keybind, rendering) lives in
 * {@code com.marie.thermalsystems.client.hud}, mirroring this mod's existing
 * {@code data.config}/{@code client.config} split.
 *
 * <p>Unlike the optional integrations in {@code integration.*}, this is core mod functionality -
 * {@link #init(IEventBus)} is called unconditionally from {@link ThermalSystemsMod}'s constructor,
 * not gated behind a {@code ModList.isLoaded} check or deferred to
 * {@code ModConfigEvent.Loading} - payload registration doesn't read any config value itself, only
 * the handler below does, and only once a request actually arrives (well after config is loaded).
 */
public final class ThermalSystemsHud {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThermalSystemsHud.class);
    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    private ThermalSystemsHud() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, ThermalSystemsHud::onRegisterPayloadHandlers);
    }

    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ThermalSystemsMod.MOD_ID).versioned("1");
        registrar.playToServer(SystemToggleRequestPayload.TYPE, SystemToggleRequestPayload.STREAM_CODEC,
                ThermalSystemsHud::onSystemToggleRequest);
    }

    private static void onSystemToggleRequest(SystemToggleRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!player.hasPermissions(REQUIRED_PERMISSION_LEVEL)) {
                if (ThermalConfig.LOGGING_ENABLED.get()) {
                    LOGGER.info("[MTS] Rejected SYSTEM_ENABLED toggle from {} (insufficient permission)",
                            player.getGameProfile().getName());
                }
                player.sendSystemMessage(Component.literal("You don't have permission to toggle Thermal Systems."));
                return;
            }

            ThermalConfig.SYSTEM_ENABLED.set(payload.enabled());
            ThermalConfig.SPEC.save();
        });
    }
}
