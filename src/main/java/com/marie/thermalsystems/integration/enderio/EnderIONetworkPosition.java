package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a point on an Ender IO conduit bundle network. Unlike
 * {@link EnderIOBlockHeatSource}, which adapts a single Ender IO machine's
 * own energy state, this sums {@code getHeatOutput()} across every Thermal-
 * Systems-capable block reachable on the network this position belongs to,
 * discovered via {@link EnderIONetworkDiscovery}.
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
 */
final class EnderIONetworkPosition implements IHeatSource {

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
        return resolve().heatSum();
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
        Set<BlockPos> boundary = EnderIONetworkDiscovery.discoverBoundary(pos, level, EnderIOIntegration.CONDUIT_BLOCK_ENTITY_TYPE);

        List<Double> heatOutputs = new ArrayList<>();
        for (BlockPos boundaryPos : boundary) {
            IHeatSource heatSource = HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, boundaryPos, null, null, null);
            if (heatSource != null) {
                heatOutputs.add(heatSource.getHeatOutput());
            }
        }

        return new CacheEntry(level.getGameTime(), EnderIONetworkSum.sum(heatOutputs));
    }

    private record CacheEntry(long computedAtTick, double heatSum) {
    }
}
