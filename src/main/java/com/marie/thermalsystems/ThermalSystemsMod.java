package com.marie.thermalsystems;

import com.marie.thermalsystems.client.config.ThermalContextRegistration;
import com.marie.thermalsystems.client.config.hub.ThermalHubScreen;
import com.marie.thermalsystems.data.config.ThermalClientConfig;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.data.config.sync.ThermalConfigSync;
import com.marie.thermalsystems.hover.ThermalHoverProvider;
import com.marie.thermalsystems.hud.ThermalSystemsHud;
import com.marie.thermalsystems.integration.coldsweat.ColdSweatIntegration;
import com.marie.thermalsystems.integration.eclipticseasons.EclipticSeasonsIntegration;
import com.marie.thermalsystems.integration.enderio.EnderIOIntegration;
import com.marie.thermalsystems.integration.lso.LSOIntegration;
import com.marie.thermalsystems.integration.mekanism.MekanismIntegration;
import com.marie.thermalsystems.integration.pneumaticcraft.PneumaticCraftIntegration;
import com.marie.thermalsystems.integration.sereneseasons.SereneSeasonsIntegration;
import com.marie.thermalsystems.integration.toughasnails.ToughAsNailsIntegration;
import com.marie.thermalsystems.network.ThermalNetwork;
import com.marie.thermalsystems.radiation.SourceRadiationTickHandler;
import com.marie.thermalsystems.registry.ThermalRegistries;
import net.neoforged.neoforge.common.NeoForge;
import dev.marie.framework.api.marieapi.MarieAPI;
import dev.marie.framework.core.MarieBootstrap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Entry point for Marie's Thermal Systems. Every heat/cooling source is a
 * real block belonging to another mod, hooked via capability registration;
 * the only content this mod ships itself is the zone and thermostat tooling
 * in {@link ThermalRegistries} (Zone Gadget, thermostat tablet, wall
 * thermostat), whose payloads {@link ThermalNetwork} handles. Event listeners
 * self-register via {@code @EventBusSubscriber}
 * ({@link com.marie.thermalsystems.climate.ClimateTickHandler},
 * {@link com.marie.thermalsystems.registry.ThermalCommands}), so this class
 * only wires config and optional integrations. PneumaticCraftIntegration,
 * MekanismIntegration, EnderIOIntegration, LSOIntegration, and
 * ColdSweatIntegration are only ever
 * loaded when their respective mods are present - the ModList checks below
 * must stay guards around the calls, never direct class references, so the
 * mod still loads with any of them absent. Each integration's own
 * {@code init()} additionally checks its config-level {@code enabled} flag
 * (e.g. {@link ThermalConfig#MEKANISM_ENABLED}) and skips all of its
 * registration - capabilities, commands, bridges, hover providers - if
 * disabled; that flag is independent of the {@code ModList} presence check
 * here, so both the mod being installed and the integration being enabled
 * are required for it to activate. Reading any {@code ThermalConfig} value
 * before NeoForge's config-loading lifecycle event fires throws
 * {@code IllegalStateException}, and that event fires after mod
 * construction - so the integration inits, which each check an
 * {@code enabled} flag as their first line, cannot run in the constructor.
 * They're deferred to a {@link ModConfigEvent.Loading} listener instead.
 *
 * <p>{@link ThermalSystemsHud#init(IEventBus)} is the one exception to "config and optional
 * integrations" - it registers {@code SystemToggleRequestPayload} for the persistent Thermal
 * Systems control panel HUD, core mod functionality rather than a foreign-mod integration, so it's
 * called unconditionally here rather than from {@link #initIntegrations(IEventBus, SourceRadiationTickHandler)}.
 */
@Mod(ThermalSystemsMod.MOD_ID)
public class ThermalSystemsMod {

    public static final String MOD_ID = "thermalsystems";

    public ThermalSystemsMod(IEventBus modEventBus, ModContainer modContainer) {
        MarieBootstrap.attachFrameworkServices(modEventBus);
        MarieAPI.registerBlockHoverProvider(new ThermalHoverProvider());
        ThermalContextRegistration.register(MOD_ID);
        ThermalSystemsHud.init(modEventBus);
        ThermalRegistries.register(modEventBus);
        ThermalNetwork.init(modEventBus);

        SourceRadiationTickHandler radiationHandler = new SourceRadiationTickHandler();
        NeoForge.EVENT_BUS.register(radiationHandler);
        ThermalConfigSync.init(modEventBus, radiationHandler);

        modContainer.registerConfig(ModConfig.Type.COMMON, ThermalConfig.SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, ThermalClientConfig.SPEC);
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (minecraft, parent) -> ThermalHubScreen.create(parent));
        }

        modEventBus.addListener((ModConfigEvent.Loading event) -> {
            if (event.getConfig().getSpec() != ThermalConfig.SPEC) {
                return;
            }
            initIntegrations(modEventBus, radiationHandler);
        });
    }

    private static void initIntegrations(IEventBus modEventBus, SourceRadiationTickHandler radiationHandler) {
        if (ModList.get().isLoaded(PneumaticCraftIntegration.PNC_MOD_ID)) {
            PneumaticCraftIntegration.init(modEventBus);
        }

        if (ModList.get().isLoaded(MekanismIntegration.MEKANISM_MOD_ID)) {
            MekanismIntegration.init(modEventBus);
        }

        if (ModList.get().isLoaded(EnderIOIntegration.ENDERIO_MOD_ID)) {
            EnderIOIntegration.init(modEventBus);
        }

        if (ModList.get().isLoaded(LSOIntegration.LSO_MOD_ID)) {
            LSOIntegration.init();
        }

        if (ModList.get().isLoaded(ColdSweatIntegration.COLDSWEAT_MOD_ID)) {
            ColdSweatIntegration.init();
        }

        if (ModList.get().isLoaded(ToughAsNailsIntegration.TOUGHASNAILS_MOD_ID)) {
            ToughAsNailsIntegration.init(modEventBus, radiationHandler);
        }

        // Ecliptic first: when both season mods are installed, its solar-term climate wins.
        if (ModList.get().isLoaded(EclipticSeasonsIntegration.MOD_ID)) {
            EclipticSeasonsIntegration.init();
        }
        if (ModList.get().isLoaded(SereneSeasonsIntegration.MOD_ID)) {
            SereneSeasonsIntegration.init();
        }
    }
}
