package com.marie.thermalsystems.climate;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schedules simulation updates. Listens for the server tick, and once every
 * {@code simulationTickInterval} ticks, calls {@link ClimateManager} to
 * advance all real zones.
 */
@EventBusSubscriber(modid = ThermalSystemsMod.MOD_ID)
public final class ClimateTickHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClimateTickHandler.class);
    private static final int TICKS_PER_SECOND = 20;

    private static int tickCounter = 0;

    private ClimateTickHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int interval = ThermalConfig.SIMULATION_TICK_INTERVAL.get();
        tickCounter++;
        if (tickCounter < interval) {
            return;
        }
        tickCounter = 0;

        double deltaTime = interval / (double) TICKS_PER_SECOND;
        var results = ClimateManager.get().advanceAll(deltaTime);

        if (ThermalConfig.LOGGING_ENABLED.get()) {
            for (ZoneAdvanceResult result : results) {
                LOGGER.info("[MTS] Zone={} Current={} Target={} Heat={}",
                        result.zone().getName(),
                        String.format("%.2f", result.zone().getCurrentTemp()),
                        String.format("%.2f", result.zone().getTargetTemp()),
                        result.totalHeatOutput());
            }
        }
    }
}
