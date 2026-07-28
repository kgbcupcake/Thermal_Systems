package com.marie.thermalsystems.zone;

import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Automatically binds/unbinds foreign heat and cooling source capabilities
 * found within a zone's bounds (expanded by {@code sourceBindingRadius}) to
 * that zone, replacing manual {@code /thermal <mod> bind} as the primary
 * binding path. This mod owns no adapter block entities for the
 * PneumaticCraft/Ender IO/Mekanism integrations - capabilities are attached
 * directly onto those mods' own block entities - so scanning happens from
 * the zone's side on a global interval, not from any block's own tick.
 *
 * <p>Cost control: a zone's full expanded volume is walked only once, the
 * first time this scanner sees it. Every following scan only walks the
 * "shell" added by the radius expansion around the zone's own bounds, since
 * the interior was already covered by that first pass and new interior
 * placements are rare and low-stakes for ambient exchanger discovery. This
 * trades missing newly-placed interior sources for avoiding an unbounded
 * full-volume walk on every scan interval.
 */
public final class ZoneSourceScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZoneSourceScanner.class);

    private static final Map<UUID, Set<BlockPos>> BOUND_HEAT_POSITIONS = new HashMap<>();
    private static final Map<UUID, Set<BlockPos>> BOUND_COOLING_POSITIONS = new HashMap<>();
    private static final Set<UUID> FULLY_SCANNED = new HashSet<>();

    private ZoneSourceScanner() {
    }

    /**
     * Clears all locally-tracked bind state. Does not touch
     * {@link ThermalSystemsAPI}'s own bound-source maps - called on server
     * stop, when those are about to disappear anyway.
     */
    public static void clearCache() {
        BOUND_HEAT_POSITIONS.clear();
        BOUND_COOLING_POSITIONS.clear();
        FULLY_SCANNED.clear();
    }

    public static void scan(ServerLevel level, ClimateZone zone, int radius) {
        if (!zone.hasBounds()) {
            unbindAll(level, zone);
            FULLY_SCANNED.remove(zone.getId());
            return;
        }

        BlockPos innerMin = zone.getBoundsMin();
        BlockPos innerMax = zone.getBoundsMax();
        BlockPos outerMin = innerMin.offset(-radius, -radius, -radius);
        BlockPos outerMax = innerMax.offset(radius, radius, radius);

        evictOutOfRange(level, zone, outerMin, outerMax);

        boolean firstScan = FULLY_SCANNED.add(zone.getId());
        if (firstScan) {
            scanRange(level, zone, outerMin, outerMax);
        } else {
            scanShell(level, zone, innerMin, innerMax, outerMin, outerMax);
        }
    }

    private static void scanRange(ServerLevel level, ClimateZone zone, BlockPos min, BlockPos max) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    tryBind(level, zone, cursor.set(x, y, z));
                }
            }
        }
    }

    private static void scanShell(ServerLevel level, ClimateZone zone, BlockPos innerMin, BlockPos innerMax,
                                   BlockPos outerMin, BlockPos outerMax) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = outerMin.getX(); x <= outerMax.getX(); x++) {
            boolean xInside = x >= innerMin.getX() && x <= innerMax.getX();
            for (int y = outerMin.getY(); y <= outerMax.getY(); y++) {
                boolean yInside = y >= innerMin.getY() && y <= innerMax.getY();
                for (int z = outerMin.getZ(); z <= outerMax.getZ(); z++) {
                    boolean zInside = z >= innerMin.getZ() && z <= innerMax.getZ();
                    if (xInside && yInside && zInside) {
                        continue;
                    }
                    tryBind(level, zone, cursor.set(x, y, z));
                }
            }
        }
    }

    private static void tryBind(ServerLevel level, ClimateZone zone, BlockPos pos) {
        UUID zoneId = zone.getId();
        Set<BlockPos> boundHeat = BOUND_HEAT_POSITIONS.computeIfAbsent(zoneId, k -> new HashSet<>());
        if (!boundHeat.contains(pos)
                && HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, pos, null, null, null) != null) {
            try {
                ThermalSystemsAPI.bindHeatSource(level, pos, zoneId);
                boundHeat.add(pos.immutable());
                logBound(zone, pos);
            } catch (IllegalArgumentException ignored) {
                // already bound elsewhere - manual override or another zone
            }
        }

        Set<BlockPos> boundCooling = BOUND_COOLING_POSITIONS.computeIfAbsent(zoneId, k -> new HashSet<>());
        if (!boundCooling.contains(pos)
                && CoolingSourceCapabilities.COOLING_SOURCE.getCapability(level, pos, null, null, null) != null) {
            try {
                ThermalSystemsAPI.bindCoolingSource(level, pos, zoneId);
                boundCooling.add(pos.immutable());
                logBound(zone, pos);
            } catch (IllegalArgumentException ignored) {
                // already bound elsewhere - manual override or another zone
            }
        }
    }

    private static void evictOutOfRange(ServerLevel level, ClimateZone zone, BlockPos outerMin, BlockPos outerMax) {
        evictSet(level, zone, BOUND_HEAT_POSITIONS.get(zone.getId()), outerMin, outerMax, true);
        evictSet(level, zone, BOUND_COOLING_POSITIONS.get(zone.getId()), outerMin, outerMax, false);
    }

    private static void evictSet(ServerLevel level, ClimateZone zone, Set<BlockPos> tracked, BlockPos outerMin,
                                  BlockPos outerMax, boolean heat) {
        if (tracked == null || tracked.isEmpty()) {
            return;
        }
        Set<BlockPos> toRemove = new HashSet<>();
        for (BlockPos pos : tracked) {
            boolean withinBox = pos.getX() >= outerMin.getX() && pos.getX() <= outerMax.getX()
                    && pos.getY() >= outerMin.getY() && pos.getY() <= outerMax.getY()
                    && pos.getZ() >= outerMin.getZ() && pos.getZ() <= outerMax.getZ();
            boolean stillPresent = withinBox && (heat
                    ? HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, pos, null, null, null) != null
                    : CoolingSourceCapabilities.COOLING_SOURCE.getCapability(level, pos, null, null, null) != null);
            if (!stillPresent) {
                if (heat) {
                    ThermalSystemsAPI.unbindHeatSource(level, pos);
                } else {
                    ThermalSystemsAPI.unbindCoolingSource(level, pos);
                }
                toRemove.add(pos);
                logUnbound(zone, pos);
            }
        }
        tracked.removeAll(toRemove);
    }

    private static void unbindAll(ServerLevel level, ClimateZone zone) {
        Set<BlockPos> heat = BOUND_HEAT_POSITIONS.remove(zone.getId());
        if (heat != null) {
            for (BlockPos pos : heat) {
                ThermalSystemsAPI.unbindHeatSource(level, pos);
                logUnbound(zone, pos);
            }
        }
        Set<BlockPos> cooling = BOUND_COOLING_POSITIONS.remove(zone.getId());
        if (cooling != null) {
            for (BlockPos pos : cooling) {
                ThermalSystemsAPI.unbindCoolingSource(level, pos);
                logUnbound(zone, pos);
            }
        }
    }

    private static void logBound(ClimateZone zone, BlockPos pos) {
        if (ThermalConfig.LOGGING_ENABLED.get() && ThermalConfig.BINDING_LOGGING_ENABLED.get()) {
            LOGGER.info("[MTS] Zone={} auto-bound source at {} (radius={})",
                    zone.getName(), pos, ThermalConfig.SOURCE_BINDING_RADIUS.get());
        }
    }

    private static void logUnbound(ClimateZone zone, BlockPos pos) {
        if (ThermalConfig.LOGGING_ENABLED.get() && ThermalConfig.BINDING_LOGGING_ENABLED.get()) {
            LOGGER.info("[MTS] Zone={} auto-unbound source at {} (out of range)", zone.getName(), pos);
        }
    }
}
