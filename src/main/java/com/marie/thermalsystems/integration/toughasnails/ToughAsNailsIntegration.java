package com.marie.thermalsystems.integration.toughasnails;

import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.radiation.SourceRadiationTickHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import toughasnails.api.temperature.TemperatureHelper;

/**
 * Optional integration with Tough As Nails. Only ever initialized when Tough
 * As Nails is present - see the {@code ModList.isLoaded} guard around
 * {@link #init} in {@link com.marie.thermalsystems.ThermalSystemsMod}. Tough
 * As Nails imports exist only within this package; nothing outside
 * {@code integration/toughasnails/} may reference them.
 *
 * <p>Like the LSO and Cold Sweat integrations, this one is consumer-only: it
 * registers {@link ToughAsNailsBridge} so the ambient temperature Thermal
 * Systems already computes per player reaches Tough As Nails. It also adds one
 * line per player to {@code /thermal debug radiation} (owned by
 * {@link SourceRadiationTickHandler}) reporting what the bridge is doing.
 */
public final class ToughAsNailsIntegration {

    public static final String TOUGHASNAILS_MOD_ID = "toughasnails";

    private ToughAsNailsIntegration() {
    }

    public static void init(IEventBus modEventBus, SourceRadiationTickHandler radiationHandler) {
        if (!ThermalConfig.TOUGHASNAILS_ENABLED.get()) {
            return;
        }
        ToughAsNailsBridge bridge = new ToughAsNailsBridge();
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) ->
                bridge.clearPlayer(event.getEntity().getUUID()));
        radiationHandler.addDebugLineProvider(bridge::debugLine);
        ThermalSystemsAPI.registerTemperatureBridge(bridge);
        modEventBus.addListener((FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> TemperatureHelper.registerPlayerTemperatureModifier(bridge::modify)));
    }
}
