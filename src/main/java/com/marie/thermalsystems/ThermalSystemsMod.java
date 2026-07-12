package com.marie.thermalsystems;

import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.integration.enderio.EnderIOIntegration;
import com.marie.thermalsystems.integration.mekanism.MekanismIntegration;
import com.marie.thermalsystems.integration.pneumaticcraft.PneumaticCraftIntegration;
import com.marie.thermalsystems.registry.ModBlockEntities;
import com.marie.thermalsystems.registry.ModBlocks;
import com.marie.thermalsystems.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * Entry point for Marie's Thermal Systems. Event listeners self-register via
 * {@code @EventBusSubscriber} ({@link com.marie.thermalsystems.climate.ClimateTickHandler},
 * {@link com.marie.thermalsystems.steam.SteamNetworkManager},
 * {@link com.marie.thermalsystems.registry.ThermalCommands}), so this class only wires config,
 * the DeferredRegisters, and optional integrations. PneumaticCraftIntegration,
 * EnderIOIntegration, and MekanismIntegration are only ever loaded when their respective mods
 * are present - the ModList checks below must stay guards around the calls, never direct class
 * references, so the mod still loads with any of them absent.
 */
@Mod(ThermalSystemsMod.MOD_ID)
public class ThermalSystemsMod {

    public static final String MOD_ID = "thermalsystems";

    public ThermalSystemsMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ThermalConfig.SPEC);

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);

        if (ModList.get().isLoaded(PneumaticCraftIntegration.PNC_MOD_ID)) {
            PneumaticCraftIntegration.init(modEventBus);
        }

        if (ModList.get().isLoaded(EnderIOIntegration.ENDERIO_MOD_ID)) {
            EnderIOIntegration.init(modEventBus);
        }

        if (ModList.get().isLoaded(MekanismIntegration.MEKANISM_MOD_ID)) {
            MekanismIntegration.init(modEventBus);
        }
    }
}
