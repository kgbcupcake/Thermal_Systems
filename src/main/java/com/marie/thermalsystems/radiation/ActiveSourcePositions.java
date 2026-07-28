package com.marie.thermalsystems.radiation;

import com.marie.thermalsystems.data.config.ThermalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic, in-memory tracker of block positions currently known to expose a
 * {@code HeatSourceCapabilities.HEAT_SOURCE}/{@code CoolingSourceCapabilities.COOLING_SOURCE}
 * capability, independent of any {@code ClimateZone}, zone registry, or
 * bind/unbind command. Knows nothing about which mod owns a given position
 * or how it got here - any integration may populate it as positions become
 * available, the way {@code EnderIOIntegration}'s chunk load/unload
 * listeners do today for Ender IO's {@code stirling_generator}/
 * {@code conduit} block entities. Consumed generically by
 * {@link SourceRadiationTickHandler} to radiate heat/cooling directly to
 * nearby players.
 *
 * <p>There is no world-scannable registry of capability-bearing positions in
 * general (capabilities are attached onto a foreign mod's own
 * {@code BlockEntityType} via {@code RegisterCapabilitiesEvent}, resolvable
 * only by querying a known position - see {@code EnderIOBlockHeatSource}),
 * so this exists purely to remember positions a populator has already
 * identified. Keyed by level, mirroring the recompute caches elsewhere in
 * this mod.
 */
public final class ActiveSourcePositions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveSourcePositions.class);

    private static final Map<ResourceKey<Level>, Set<BlockPos>> POSITIONS = new ConcurrentHashMap<>();

    private ActiveSourcePositions() {
    }

    public static void add(Level level, BlockPos pos) {
        BlockPos immutablePos = pos.immutable();
        boolean added = POSITIONS.computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet()).add(immutablePos);
        if (ThermalConfig.LOGGING_ENABLED.get() && ThermalConfig.RADIATION_LOGGING_ENABLED.get()) {
            LOGGER.info("[MTS] ActiveSourcePositions add dim={} pos={} newlyAdded={}",
                    level.dimension().location(), immutablePos, added);
        }
    }

    public static void remove(Level level, BlockPos pos) {
        Set<BlockPos> positions = POSITIONS.get(level.dimension());
        boolean removed = positions != null && positions.remove(pos);
        if (ThermalConfig.LOGGING_ENABLED.get() && ThermalConfig.RADIATION_LOGGING_ENABLED.get()) {
            LOGGER.info("[MTS] ActiveSourcePositions remove dim={} pos={} wasPresent={}",
                    level.dimension().location(), pos.immutable(), removed);
        }
    }

    public static Set<BlockPos> getAll(Level level) {
        return POSITIONS.getOrDefault(level.dimension(), Collections.emptySet());
    }

    static void clear() {
        POSITIONS.clear();
    }
}
