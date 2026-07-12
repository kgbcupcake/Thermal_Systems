package com.marie.thermalsystems.steam;

import net.minecraft.core.BlockPos;

import java.util.Set;

/**
 * Immutable snapshot of one connected steam network's membership. Block
 * entities can become invalid between ticks, so a network holds positions
 * only; consumers resolve each BlockPos to a live block entity via a world
 * lookup at evaluation time and verify its type, never caching the resolved
 * entity.
 */
public record SteamNetwork(Set<BlockPos> pipes, Set<BlockPos> boilers, Set<BlockPos> radiators) {
}
