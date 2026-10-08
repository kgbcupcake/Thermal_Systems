package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.api.climate.IHeatPump;
import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.climate.ClimateManager;
import com.marie.thermalsystems.controller.ClimateDemand;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.zone.ClimateZone;
import com.marie.thermalsystems.zone.ZoneSpatialIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a point on an Ender IO conduit bundle network. Unlike
 * {@link EnderIOBlockHeatSource}, which adapts a single Ender IO machine's
 * own energy state, this sums {@code getHeatOutput()}/{@code getCoolingOutput()}
 * across every Thermal-Systems-capable block reachable on the network this
 * position belongs to, discovered via {@link EnderIONetworkDiscovery}.
 *
 * <p>The NeoForge capability system calls the registered provider fresh on
 * every {@code getCapability()} query, so no per-instance state survives
 * between evaluations; the recompute-interval cache below is therefore
 * static and keyed by level + position, mirroring the dirty/interval shape
 * the now-removed {@code SteamNetworkManager} used, but scoped lazily to
 * positions actually queried (bound to a zone) rather than proactively
 * tracking every conduit in the world. Every entry is keyed by the pair of
 * {@link ResourceKey}&lt;{@link Level}&gt; and {@link BlockPos} it was
 * computed for, so two different positions or two different dimensions can
 * never read each other's cached sum. The one gap a bare static field would
 * otherwise have - stale entries surviving a level being unloaded and a
 * different one reusing the same dimension key and position within the same
 * JVM (e.g. singleplayer world-switching) - is closed by {@link #clearCache()},
 * wired to {@code ServerStoppingEvent} in {@link EnderIOIntegration#init}, so
 * this cache's lifetime is tied to the NeoForge server lifecycle rather than
 * only to the classloader, per the project's no-unmanaged-global-state rule.
 *
 * <p>Machines on the network that are {@link IHeatPump}s (the Stirling
 * Generator) are summed by what they can deliver, and the whole network then
 * runs in the direction this position's zone thermostat calls for - so a
 * generator in the basement feeding conduits into a room follows that room.
 * A position outside every zone follows {@link #networkZone()} instead. The
 * cache holds those raw sums and the network's zone; the direction is
 * applied per query.
 */
final class EnderIONetworkPosition implements IHeatSource, ICoolingSource, IHeatPump {

    private static final Map<ResourceKey<Level>, Map<BlockPos, CacheEntry>> CACHE = new ConcurrentHashMap<>();

    static void clearCache() {
        CACHE.clear();
    }

    private final Level level;
    private final BlockPos pos;

    EnderIONetworkPosition(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    @Override
    public double getHeatOutput() {
        CacheEntry entry = resolve();
        double pumped = entry.pumpSum() > 0.0
                && EnderIOBlockHeatSource.direction(getControllingZone()) == ClimateDemand.HEATING
                ? entry.pumpSum() : 0.0;
        return entry.heatSum() + pumped;
    }

    @Override
    public double getCoolingOutput() {
        CacheEntry entry = resolve();
        double pumped = entry.pumpSum() > 0.0
                && EnderIOBlockHeatSource.direction(getControllingZone()) == ClimateDemand.COOLING
                ? entry.pumpSum() : 0.0;
        return entry.coolingSum() + pumped;
    }

    @Override
    public double getPumpOutput() {
        return resolve().pumpSum();
    }

    /** The zone containing this position, or else the zone the rest of its network is in. */
    @Override
    public Optional<ClimateZone> getControllingZone() {
        Optional<ClimateZone> own = ZoneSpatialIndex.resolve(level, pos);
        return own.isPresent() ? own : networkZone();
    }

    /**
     * The zone holding one of the network's heat pumps (its generators), or failing that one of its
     * conduits - so conduits run under a floor or through a wall just outside a room still follow
     * the room their generator heats.
     */
    Optional<ClimateZone> networkZone() {
        UUID id = resolve().networkZoneId();
        return id == null ? Optional.empty() : ClimateManager.get().getZone(level.dimension(), id);
    }

    /**
     * A stable identity for the network this position belongs to, used by
     * {@code SourceRadiationTickHandler} to sum a network's heat/cooling
     * exactly once even when several of its positions are simultaneously in
     * range of a player. The lowest {@link BlockPos} (by natural,
     * coordinate-wise ordering) among the discovered network's own conduit
     * membership - not the boundary - so it comes out identical regardless
     * of which conduit on the network this instance happens to wrap, and
     * {@code null} only in the pathological case where {@code pos} no longer
     * resolves to a conduit at all (network already gone).
     */
    @Override
    public Object getNetworkId() {
        return resolve().networkId();
    }

    private CacheEntry resolve() {
        Map<BlockPos, CacheEntry> levelCache = CACHE.computeIfAbsent(level.dimension(), key -> new ConcurrentHashMap<>());
        long now = level.getGameTime();
        int interval = ThermalConfig.ENDERIO_NETWORK_RECOMPUTE_INTERVAL.get();

        CacheEntry existing = levelCache.get(pos);
        if (existing != null && now - existing.computedAtTick() < interval) {
            return existing;
        }

        CacheEntry recomputed = recompute();
        levelCache.put(pos, recomputed);
        return recomputed;
    }

    private CacheEntry recompute() {
        EnderIONetworkDiscovery.NetworkResult network =
                EnderIONetworkDiscovery.discover(pos, level, EnderIOIntegration.CONDUIT_BLOCK_ENTITY_TYPE);

        List<Double> heatOutputs = new ArrayList<>();
        List<Double> coolingOutputs = new ArrayList<>();
        List<Double> pumpOutputs = new ArrayList<>();
        List<BlockPos> pumpPositions = new ArrayList<>();
        for (BlockPos boundaryPos : network.boundary()) {
            IHeatSource heatSource = HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, boundaryPos, null, null, null);
            ICoolingSource coolingSource = CoolingSourceCapabilities.COOLING_SOURCE.getCapability(level, boundaryPos, null, null, null);
            IHeatPump pump = heatSource instanceof IHeatPump p ? p : coolingSource instanceof IHeatPump p ? p : null;
            if (pump != null) {
                pumpOutputs.add(pump.getPumpOutput());
                pumpPositions.add(boundaryPos);
                continue;
            }
            if (heatSource != null) {
                heatOutputs.add(heatSource.getHeatOutput());
            }
            if (coolingSource != null) {
                coolingOutputs.add(coolingSource.getCoolingOutput());
            }
        }

        BlockPos networkId = network.conduits().stream().min(BlockPos::compareTo).orElse(null);
        UUID networkZoneId = ZoneSpatialIndex.firstContainingAny(level, pumpPositions)
                .or(() -> ZoneSpatialIndex.firstContainingAny(level, network.conduits()))
                .map(ClimateZone::getId)
                .orElse(null);

        return new CacheEntry(level.getGameTime(), EnderIONetworkSum.sum(heatOutputs), EnderIONetworkSum.sum(coolingOutputs),
                EnderIONetworkSum.sum(pumpOutputs), networkId, networkZoneId);
    }

    /** {@code heatSum}/{@code coolingSum} are non-pump machines; {@code pumpSum} is direction-free. */
    private record CacheEntry(long computedAtTick, double heatSum, double coolingSum, double pumpSum, BlockPos networkId,
                              @Nullable UUID networkZoneId) {
    }
}
