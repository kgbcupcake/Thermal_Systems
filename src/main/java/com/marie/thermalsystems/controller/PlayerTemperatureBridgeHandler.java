package com.marie.thermalsystems.controller;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.bridge.ITemperatureBridge;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.zone.ClimateZone;
import com.marie.thermalsystems.zone.ZoneSpatialIndex;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Drives the consumer side of the bridge API. Once per
 * {@code playerBridgeInterval} server ticks, resolves every online player's
 * zone via {@link ZoneSpatialIndex}, falls back to
 * {@code defaultAmbientTemperature} if unresolved, and invokes every
 * registered {@link ITemperatureBridge}. Bridges themselves do nothing but
 * receive a number.
 */
@EventBusSubscriber(modid = ThermalSystemsMod.MOD_ID)
public final class PlayerTemperatureBridgeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerTemperatureBridgeHandler.class);

    /**
     * Fixed, unique-to-this-caller id passed as {@link ITemperatureBridge}'s
     * {@code sourceId} - see that interface's Javadoc. Identifies every call
     * this handler makes as the "zone-ambient temperature" contribution, kept
     * distinct from {@code SourceRadiationTickHandler}'s own id so a bridge
     * backed by a caller-keyed additive attribute system (like LSO) doesn't
     * let one caller's contribution silently overwrite the other's. Never
     * regenerate this value.
     */
    private static final UUID SOURCE_ID = UUID.fromString("8f2e6f2c-9b1a-4a3e-9d3a-1c6a8e5f2b7d");

    private static int tickCounter = 0;
    private static Boolean wasEnabled = null;

    private PlayerTemperatureBridgeHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        boolean enabled = ThermalConfig.SYSTEM_ENABLED.get();
        logIfEnabledChanged(enabled);
        if (!enabled) {
            tickCounter = 0;
            return;
        }

        int interval = ThermalConfig.PLAYER_BRIDGE_INTERVAL.get();
        tickCounter++;
        if (tickCounter < interval) {
            return;
        }
        tickCounter = 0;

        List<ITemperatureBridge> bridges = ThermalSystemsAPI.getTemperatureBridges();
        if (bridges.isEmpty()) {
            return;
        }

        double defaultAmbient = ThermalConfig.DEFAULT_AMBIENT_TEMPERATURE.get();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                Optional<ClimateZone> zone = ZoneSpatialIndex.resolve(level, player.blockPosition());
                double temperature = zone.map(ClimateZone::getCurrentTemp).orElse(defaultAmbient);
                for (ITemperatureBridge bridge : bridges) {
                    bridge.applyAmbientTemperature(player, temperature, SOURCE_ID);
                }
            }
        }
    }

    private static void logIfEnabledChanged(boolean enabled) {
        Boolean previous = wasEnabled;
        wasEnabled = enabled;
        if (previous == null || previous.booleanValue() == enabled || !ThermalConfig.LOGGING_ENABLED.get()) {
            return;
        }
        if (enabled) {
            LOGGER.info("[MTS] PlayerTemperatureBridgeHandler simulation resumed (SYSTEM_ENABLED=true)");
        } else {
            LOGGER.info("[MTS] PlayerTemperatureBridgeHandler simulation paused (SYSTEM_ENABLED=false)");
        }
    }
}
