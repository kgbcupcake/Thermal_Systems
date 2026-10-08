package com.marie.thermalsystems.zone;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.climate.ClimateManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Loads each dimension's saved zones when its level loads, and clears all in-memory zone and
 * binding state once the server has fully stopped. Clearing waits for {@link ServerStoppedEvent}
 * rather than {@code ServerStoppingEvent} because levels are saved after the latter fires - zones
 * dropped any earlier would be written out empty.
 */
@EventBusSubscriber(modid = ThermalSystemsMod.MOD_ID)
public final class ZonePersistence {

    private ZonePersistence() {
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ZoneSavedData.loadInto(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ClimateManager.get().clear();
        ThermalSystemsAPI.clearAllBindings();
    }
}
