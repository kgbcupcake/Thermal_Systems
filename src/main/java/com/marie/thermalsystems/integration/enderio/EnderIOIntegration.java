package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.registry.ModBlockEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Optional integration with Ender IO. Only ever loaded and initialized when
 * Ender IO is present - see the {@code ModList.isLoaded} guard around
 * {@link #init(IEventBus)} in {@link com.marie.thermalsystems.ThermalSystemsMod}.
 * No Ender IO classes are imported anywhere in this package: connectivity is
 * governed entirely by the three standard NeoForge capabilities Ender IO's
 * conduits key off, registered directly onto this mod's own, unmodified
 * {@code BoilerBlockEntity} and {@code RadiatorBlockEntity} types.
 *
 * <p>Verified against the Ender IO 8.2.11-beta jar (the current release for
 * 1.21.1/NeoForge 21.1.x - see {@code libs/enderio-8.2.11-beta.jar} and the
 * {@code compileOnly} dependency in {@code build.gradle}; this is a
 * substantial rewrite of Ender IO's conduit engine compared to what earlier
 * phases assumed, under the {@code com.enderio.enderio.content.conduits}
 * package rather than the old {@code com.enderio.EnderIO} one). Disassembling
 * each conduit's {@code canConnectToBlock} confirms all three still key off
 * plain capability presence, with no additional gating:
 * <ul>
 *   <li>{@code EnergyConduit.canConnectToBlock} queries
 *       {@link Capabilities.EnergyStorage#BLOCK}.</li>
 *   <li>{@code FluidConduit.canConnectToBlock} queries
 *       {@link Capabilities.FluidHandler#BLOCK}.</li>
 *   <li>{@code ItemConduit.canConnectToBlock} queries
 *       {@link Capabilities.ItemHandler#BLOCK}.</li>
 * </ul>
 * Each simply checks whether {@code ConduitCapabilityAccessor.getSidedCapability}
 * returns non-null - matching the Phase 5 finding for
 * {@code EnergyConduit} exactly, now confirmed for Fluid and Item too.
 */
public final class EnderIOIntegration {

    public static final String ENDERIO_MOD_ID = "enderio";

    private EnderIOIntegration() {
    }

    public static void init(IEventBus modEventBus) {
        EnderIOSteamFluid.init(modEventBus);
        modEventBus.addListener(RegisterCapabilitiesEvent.class, EnderIOIntegration::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.BOILER.get(),
                (boiler, side) -> new EnderIOSteamTank(boiler::getHeatOutput, boiler::setHeatOutput));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.RADIATOR.get(),
                (radiator, side) -> new EnderIOSteamTank(radiator::getHeatOutput, radiator::setHeatShare));

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.BOILER.get(),
                (boiler, side) -> new EnderIOEnergyAdapter(boiler));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BOILER.get(),
                (boiler, side) -> new EnderIOItemAdapter());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.RADIATOR.get(),
                (radiator, side) -> new EnderIOItemAdapter());
    }
}
