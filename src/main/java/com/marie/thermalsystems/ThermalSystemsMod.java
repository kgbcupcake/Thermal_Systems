package com.marie.thermalsystems;

import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.hover.ThermalHoverProvider;
import com.marie.thermalsystems.integration.enderio.EnderIOIntegration;
import com.marie.thermalsystems.integration.enderio.EnderIONetworkHoverProvider;
import com.marie.thermalsystems.integration.mekanism.MekanismIntegration;
import com.marie.thermalsystems.integration.mekanism.MekanismNetworkHoverProvider;
import com.marie.thermalsystems.integration.pneumaticcraft.PneumaticCraftIntegration;
import com.marie.thermalsystems.integration.pneumaticcraft.PneumaticCraftTubeHoverProvider;
import dev.marie.framework.api.marieapi.MarieAPI;
import dev.marie.framework.core.MarieBootstrap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * Entry point for Marie's Thermal Systems. This mod ships no blocks or items
 * of its own - every heat/cooling source is a real block belonging to
 * another mod, hooked via capability registration. Event listeners
 * self-register via {@code @EventBusSubscriber}
 * ({@link com.marie.thermalsystems.climate.ClimateTickHandler},
 * {@link com.marie.thermalsystems.registry.ThermalCommands}), so this class
 * only wires config and optional integrations. PneumaticCraftIntegration,
 * MekanismIntegration, and EnderIOIntegration are only ever loaded when
 * their respective mods are present - the ModList checks below must stay
 * guards around the calls, never direct class references, so the mod still
 * loads with any of them absent.
 */
@Mod(ThermalSystemsMod.MOD_ID)
public class ThermalSystemsMod {

    public static final String MOD_ID = "thermalsystems";

    public ThermalSystemsMod(IEventBus modEventBus, ModContainer modContainer) {
        MarieBootstrap.attachFrameworkServices(modEventBus);
        MarieAPI.registerBlockHoverProvider(new ThermalHoverProvider());

        modContainer.registerConfig(ModConfig.Type.COMMON, ThermalConfig.SPEC);

        if (ModList.get().isLoaded(PneumaticCraftIntegration.PNC_MOD_ID)) {
            PneumaticCraftIntegration.init(modEventBus);
            MarieAPI.registerBlockHoverProvider(new PneumaticCraftTubeHoverProvider());
        }

        if (ModList.get().isLoaded(MekanismIntegration.MEKANISM_MOD_ID)) {
            MekanismIntegration.init(modEventBus);
            MarieAPI.registerBlockHoverProvider(new MekanismNetworkHoverProvider());
        }

        if (ModList.get().isLoaded(EnderIOIntegration.ENDERIO_MOD_ID)) {
            EnderIOIntegration.init(modEventBus);
            MarieAPI.registerBlockHoverProvider(new EnderIONetworkHoverProvider());
        }
    }
}
