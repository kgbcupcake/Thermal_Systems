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

import java.util.List;
import java.util.Optional;

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

    private static int tickCounter = 0;

    private PlayerTemperatureBridgeHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
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
                    bridge.applyAmbientTemperature(player, temperature);
                }
            }
        }
    }
}
