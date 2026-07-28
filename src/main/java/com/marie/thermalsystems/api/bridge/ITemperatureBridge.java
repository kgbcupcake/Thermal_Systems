package com.marie.thermalsystems.api.bridge;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Implemented by an external adapter mod (e.g. an "LSO Bridge" mod) to
 * receive a player's resolved ambient temperature once per configured
 * interval. Register an instance via
 * {@link com.marie.thermalsystems.api.ThermalSystemsAPI#registerTemperatureBridge(ITemperatureBridge)}.
 * Thermal Systems never calls into the target survival-temperature mod's
 * API directly; the adapter owns that call inside this method, which is
 * what keeps the dependency direction pointing at Thermal Systems.
 *
 * <p>Thermal Systems has more than one independent caller of this method for
 * the same player - {@code PlayerTemperatureBridgeHandler} (zone-ambient
 * temperature) and {@code SourceRadiationTickHandler} (direct radiation from
 * a nearby source) both call every registered bridge on their own interval,
 * with different numbers. {@code sourceId} is a fixed, caller-owned constant
 * that identifies which of these independent contributions a given call
 * represents - each caller uses the same {@code sourceId} on every call it
 * makes, and a different one from every other caller. An implementation
 * backed by an additive, caller-keyed attribute/modifier system (as LSO's
 * {@code TemperatureUtil.addTemperatureModifier} is - see
 * {@code LSOThermalBridge}) should use {@code sourceId} directly as that
 * key, so two callers' contributions coexist as separate modifiers instead
 * of one overwriting the other under a shared key.
 */
public interface ITemperatureBridge {

    void applyAmbientTemperature(ServerPlayer player, double ambientTemperatureCelsius, UUID sourceId);
}
