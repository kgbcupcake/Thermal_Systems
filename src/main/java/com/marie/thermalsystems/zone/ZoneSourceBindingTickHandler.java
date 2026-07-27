package com.marie.thermalsystems.zone;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.climate.ClimateManager;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Once every {@code sourceScanInterval} server ticks, scans every bounded
 * zone in every level via {@link ZoneSourceScanner} to automatically bind or
 * unbind foreign heat/cooling source capabilities within
 * {@code sourceBindingRadius} of the zone's bounds. This replaces manual
 * {@code /thermal <mod> bind} as the primary binding path; those commands
 * remain a valid override, calling the same ThermalSystemsAPI methods
 * directly.
 */
@EventBusSubscriber(modid = ThermalSystemsMod.MOD_ID)
public final class ZoneSourceBindingTickHandler {

    private static int tickCounter = 0;

    private ZoneSourceBindingTickHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int interval = ThermalConfig.SOURCE_BINDING_SCAN_INTERVAL.get();
        tickCounter++;
        if (tickCounter < interval) {
            return;
        }
        tickCounter = 0;

        int radius = ThermalConfig.SOURCE_BINDING_RADIUS.get();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ClimateZone zone : ClimateManager.get().getZones(level.dimension())) {
                ZoneSourceScanner.scan(level, zone, radius);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ZoneSourceScanner.clearCache();
    }
}
