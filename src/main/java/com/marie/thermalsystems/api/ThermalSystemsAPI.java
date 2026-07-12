package com.marie.thermalsystems.api;

import com.marie.thermalsystems.api.bridge.ITemperatureBridge;
import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.api.zone.ZoneSnapshot;
import com.marie.thermalsystems.climate.ClimateManager;
import com.marie.thermalsystems.cooling.CapabilityCoolingSource;
import com.marie.thermalsystems.heating.CapabilityHeatSource;
import com.marie.thermalsystems.zone.ClimateZone;
import com.marie.thermalsystems.zone.ZoneSpatialIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The sole public entry point for external mods integrating with Marie's
 * Thermal Systems. Every method accepts and returns only simple types
 * (Level, BlockPos, UUID, primitives, Optional) or the public records/
 * interfaces declared in this package: {@link ZoneSnapshot}, {@link IHeatSource},
 * {@link ICoolingSource}, {@link ITemperatureBridge}. Internal manager
 * classes and the live mutable ClimateZone are never exposed.
 */
public final class ThermalSystemsAPI {

    // TODO(future phase): invalidate bound-source entries when zone deletion is
    // implemented - not reachable today since zone deletion doesn't exist yet
    // (Phase 1 explicitly deferred it).
    private static final Map<ResourceKey<Level>, Map<BlockPos, BoundHeatSource>> BOUND_HEAT_SOURCES = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<BlockPos, BoundCoolingSource>> BOUND_COOLING_SOURCES = new HashMap<>();
    private static final List<ITemperatureBridge> BRIDGES = new CopyOnWriteArrayList<>();

    private ThermalSystemsAPI() {
    }

    public static Optional<ZoneSnapshot> getZone(Level level, UUID zoneId) {
        return ClimateManager.get().getZone(level.dimension(), zoneId).map(ThermalSystemsAPI::toSnapshot);
    }

    public static Optional<ZoneSnapshot> getZoneByName(Level level, String name) {
        return ClimateManager.get().getZoneByName(level.dimension(), name).map(ThermalSystemsAPI::toSnapshot);
    }

    public static Optional<ZoneSnapshot> getZoneAt(Level level, BlockPos pos) {
        return ZoneSpatialIndex.resolve(level, pos).map(ThermalSystemsAPI::toSnapshot);
    }

    /**
     * Binds the {@link IHeatSource} capability present at {@code pos} to the
     * zone identified by {@code zoneId}.
     *
     * @throws IllegalArgumentException if no heat source capability is present
     *                                   at pos, no zone with zoneId exists, or
     *                                   pos is already bound to a zone
     */
    public static void bindHeatSource(Level level, BlockPos pos, UUID zoneId) {
        ResourceKey<Level> key = level.dimension();
        BlockPos immutablePos = pos.immutable();
        Map<BlockPos, BoundHeatSource> bound = BOUND_HEAT_SOURCES.computeIfAbsent(key, k -> new HashMap<>());
        if (bound.containsKey(immutablePos)) {
            throw new IllegalArgumentException("Position " + immutablePos + " is already bound to a zone; unbind first.");
        }

        IHeatSource capability = HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, immutablePos, null, null, null);
        if (capability == null) {
            throw new IllegalArgumentException("No heat source capability present at " + immutablePos + ".");
        }

        ClimateZone zone = ClimateManager.get().getZone(key, zoneId)
                .orElseThrow(() -> new IllegalArgumentException("No climate zone with id " + zoneId + " exists."));

        CapabilityHeatSource source = new CapabilityHeatSource(level, immutablePos);
        zone.addHeatSource(source);
        bound.put(immutablePos, new BoundHeatSource(zoneId, source));
    }

    public static void unbindHeatSource(Level level, BlockPos pos) {
        ResourceKey<Level> key = level.dimension();
        Map<BlockPos, BoundHeatSource> bound = BOUND_HEAT_SOURCES.get(key);
        if (bound == null) {
            return;
        }
        BoundHeatSource entry = bound.remove(pos.immutable());
        if (entry == null) {
            return;
        }
        ClimateManager.get().getZone(key, entry.zoneId())
                .ifPresent(zone -> zone.removeHeatSource(entry.source()));
    }

    /**
     * Binds the {@link ICoolingSource} capability present at {@code pos} to
     * the zone identified by {@code zoneId}.
     *
     * @throws IllegalArgumentException if no cooling source capability is
     *                                   present at pos, no zone with zoneId
     *                                   exists, or pos is already bound to a zone
     */
    public static void bindCoolingSource(Level level, BlockPos pos, UUID zoneId) {
        ResourceKey<Level> key = level.dimension();
        BlockPos immutablePos = pos.immutable();
        Map<BlockPos, BoundCoolingSource> bound = BOUND_COOLING_SOURCES.computeIfAbsent(key, k -> new HashMap<>());
        if (bound.containsKey(immutablePos)) {
            throw new IllegalArgumentException("Position " + immutablePos + " is already bound to a zone; unbind first.");
        }

        ICoolingSource capability = CoolingSourceCapabilities.COOLING_SOURCE.getCapability(level, immutablePos, null, null, null);
        if (capability == null) {
            throw new IllegalArgumentException("No cooling source capability present at " + immutablePos + ".");
        }

        ClimateZone zone = ClimateManager.get().getZone(key, zoneId)
                .orElseThrow(() -> new IllegalArgumentException("No climate zone with id " + zoneId + " exists."));

        CapabilityCoolingSource source = new CapabilityCoolingSource(level, immutablePos);
        zone.addCoolingSource(source);
        bound.put(immutablePos, new BoundCoolingSource(zoneId, source));
    }

    public static void unbindCoolingSource(Level level, BlockPos pos) {
        ResourceKey<Level> key = level.dimension();
        Map<BlockPos, BoundCoolingSource> bound = BOUND_COOLING_SOURCES.get(key);
        if (bound == null) {
            return;
        }
        BoundCoolingSource entry = bound.remove(pos.immutable());
        if (entry == null) {
            return;
        }
        ClimateManager.get().getZone(key, entry.zoneId())
                .ifPresent(zone -> zone.removeCoolingSource(entry.source()));
    }

    public static void registerTemperatureBridge(ITemperatureBridge bridge) {
        if (!BRIDGES.contains(bridge)) {
            BRIDGES.add(bridge);
        }
    }

    public static void unregisterTemperatureBridge(ITemperatureBridge bridge) {
        BRIDGES.remove(bridge);
    }

    public static List<ITemperatureBridge> getTemperatureBridges() {
        return List.copyOf(BRIDGES);
    }

    private static ZoneSnapshot toSnapshot(ClimateZone zone) {
        return new ZoneSnapshot(zone.getId(), zone.getName(), zone.getCurrentTemp(), zone.getTargetTemp(), zone.getMode());
    }

    private record BoundHeatSource(UUID zoneId, CapabilityHeatSource source) {
    }

    private record BoundCoolingSource(UUID zoneId, CapabilityCoolingSource source) {
    }
}
