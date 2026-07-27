package com.marie.thermalsystems.integration.enderio;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks each Ender IO Stirling Generator's current {@link ThermalMode}, set
 * via the {@code /thermal enderio setMode} command in
 * {@link EnderIOIntegration}. Deliberately in-memory only and never
 * persisted to the world: that command is a temporary testing stopgap ahead
 * of a real Marie's UI smart-switch toggle, so building save/load
 * infrastructure around it now would just be thrown away later. Every
 * generator defaults to {@link ThermalMode#HEAT} until explicitly switched,
 * including after a server restart.
 *
 * <p>Keyed by level + position, mirroring {@link EnderIONetworkPosition}'s
 * cache, and cleared alongside it on {@code ServerStoppingEvent} so stale
 * entries never survive a level unloading and a different one reusing the
 * same dimension key and position within the same JVM.
 */
final class EnderIOAdapterModeRegistry {

    private static final Map<ResourceKey<Level>, Map<BlockPos, ThermalMode>> MODES = new ConcurrentHashMap<>();

    private EnderIOAdapterModeRegistry() {
    }

    static void clear() {
        MODES.clear();
    }

    static ThermalMode get(Level level, BlockPos pos) {
        Map<BlockPos, ThermalMode> levelModes = MODES.get(level.dimension());
        if (levelModes == null) {
            return ThermalMode.HEAT;
        }
        return levelModes.getOrDefault(pos, ThermalMode.HEAT);
    }

    static void set(Level level, BlockPos pos, ThermalMode mode) {
        MODES.computeIfAbsent(level.dimension(), key -> new ConcurrentHashMap<>())
                .put(pos.immutable(), mode);
    }
}
