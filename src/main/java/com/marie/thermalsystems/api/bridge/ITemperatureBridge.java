package com.marie.thermalsystems.api.bridge;

import net.minecraft.server.level.ServerPlayer;

/**
 * Implemented by an external adapter mod (e.g. an "LSO Bridge" mod) to
 * receive a player's resolved ambient temperature once per configured
 * interval. Register an instance via
 * {@link com.marie.thermalsystems.api.ThermalSystemsAPI#registerTemperatureBridge(ITemperatureBridge)}.
 * Thermal Systems never calls into the target survival-temperature mod's
 * API directly; the adapter owns that call inside this method, which is
 * what keeps the dependency direction pointing at Thermal Systems.
 */
public interface ITemperatureBridge {

    void applyAmbientTemperature(ServerPlayer player, double ambientTemperatureCelsius);
}
