package com.marie.thermalsystems.integration.coldsweat;

import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Optional integration with Cold Sweat. Only ever initialized when Cold
 * Sweat is present - see the {@code ModList.isLoaded} guard around
 * {@link #init()} in {@link com.marie.thermalsystems.ThermalSystemsMod}.
 * Cold Sweat imports exist only within this package; nothing outside
 * {@code integration/coldsweat/} may reference them.
 *
 * <p>Like {@link com.marie.thermalsystems.integration.lso.LSOIntegration},
 * this integration is consumer-only: it registers
 * {@link ColdSweatThermalBridge} so the ambient temperature Thermal Systems
 * already computes per player reaches Cold Sweat. {@code WarmthTempModifier}/
 * {@code FrigidnessTempModifier} are Cold Sweat's own built-in classes -
 * already registered by Cold Sweat itself under its own ids for NBT
 * serialization - so this integration must not re-register them under a new
 * id; Cold Sweat's {@code TempModifierRegistry.register()} treats that as a
 * duplicate class and throws (with a broken error message, an NPE, since it
 * looks the new id up under the id it was actually inserted under). No
 * {@code TempModifierRegisterEvent} subscription is needed here at all.
 */
public final class ColdSweatIntegration {

    public static final String COLDSWEAT_MOD_ID = "cold_sweat";

    private ColdSweatIntegration() {
    }

    public static void init() {
        if (!ThermalConfig.COLDSWEAT_ENABLED.get()) {
            return;
        }
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) ->
                ColdSweatThermalBridge.clearPlayer(event.getEntity().getUUID()));
        ThermalSystemsAPI.registerTemperatureBridge(new ColdSweatThermalBridge());
    }
}
