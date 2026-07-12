package com.marie.thermalsystems.api.cooling;

import com.marie.thermalsystems.ThermalSystemsMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * Declares the capability external mods register their block entities
 * against to become a Zone's cooling source, with zero compile-time
 * dependency on this mod's concrete classes.
 */
public final class CoolingSourceCapabilities {

    public static final BlockCapability<ICoolingSource, Void> COOLING_SOURCE = BlockCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(ThermalSystemsMod.MOD_ID, "cooling_source"), ICoolingSource.class);

    private CoolingSourceCapabilities() {
    }
}
