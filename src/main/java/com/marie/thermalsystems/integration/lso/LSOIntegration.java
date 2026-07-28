package com.marie.thermalsystems.integration.lso;

import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.data.config.ThermalConfig;

/**
 * Optional integration with Legendary Survival Overhaul. Only ever
 * initialized when LSO is present - see the {@code ModList.isLoaded} guard
 * around {@link #init()} in {@link com.marie.thermalsystems.ThermalSystemsMod}.
 * LSO imports exist only within this package; nothing outside
 * {@code integration/lso/} may reference them.
 *
 * <p>Unlike the block/network integrations in sibling packages, this
 * integration is consumer-only: it registers {@link LSOThermalBridge} so the
 * ambient temperature Thermal Systems already computes per player (see
 * {@link com.marie.thermalsystems.controller.PlayerTemperatureBridgeHandler})
 * reaches LSO. It introduces no blocks, capabilities, or commands.
 */
public final class LSOIntegration {

    public static final String LSO_MOD_ID = "legendarysurvivaloverhaul";

    private LSOIntegration() {
    }

    public static void init() {
        if (!ThermalConfig.LSO_ENABLED.get()) {
            return;
        }
        ThermalSystemsAPI.registerTemperatureBridge(new LSOThermalBridge());
    }
}
