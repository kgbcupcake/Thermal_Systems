package com.marie.thermalsystems.zone;

import com.marie.thermalsystems.climate.ClimateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Optional;

/**
 * Resolves which bound ClimateZone, if any, contains a given position. Pure
 * lookup: never mutates zone state. If a position falls inside more than one
 * zone's bounds, returns the zone that registered its bounds first, matching
 * ZoneRegistry's existing deterministic iteration order.
 */
public final class ZoneSpatialIndex {

    private ZoneSpatialIndex() {
    }

    public static Optional<ClimateZone> resolve(Level level, BlockPos pos) {
        ResourceKey<Level> key = level.dimension();
        return resolveAmong(ClimateManager.get().getZones(key), pos);
    }

    /**
     * Pure containment resolution over an explicit zone collection, split out
     * from {@link #resolve(Level, BlockPos)} so the tie-break logic is
     * testable without a live Level.
     */
    static Optional<ClimateZone> resolveAmong(Collection<ClimateZone> zones, BlockPos pos) {
        for (ClimateZone zone : zones) {
            if (zone.containsPosition(pos)) {
                return Optional.of(zone);
            }
        }
        return Optional.empty();
    }
}
