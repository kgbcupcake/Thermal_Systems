package com.marie.thermalsystems.api.heating;

import com.marie.thermalsystems.ThermalSystemsMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * Declares the capability external mods register their block entities
 * against to become a Zone's heat source, with zero compile-time dependency
 * on this mod's concrete classes.
 */
public final class HeatSourceCapabilities {

    public static final BlockCapability<IHeatSource, Void> HEAT_SOURCE = BlockCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(ThermalSystemsMod.MOD_ID, "heat_source"), IHeatSource.class);

    private HeatSourceCapabilities() {
    }
}
