package com.marie.thermalsystems.registry;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Registers this mod's own block entities against the public capability API
 * declared in {@code api/heating} and {@code api/cooling}. This is a
 * discoverability registration only - RadiatorBlockEntity's direct
 * {@code IHeatSource} implementation (Phase 2) is unchanged.
 */
@EventBusSubscriber(modid = ThermalSystemsMod.MOD_ID)
public final class ModCapabilities {

    private ModCapabilities() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                HeatSourceCapabilities.HEAT_SOURCE,
                ModBlockEntities.RADIATOR.get(),
                (radiator, context) -> radiator);
    }
}
