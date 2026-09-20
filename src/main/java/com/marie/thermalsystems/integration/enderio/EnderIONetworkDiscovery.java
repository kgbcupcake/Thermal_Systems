package com.marie.thermalsystems.integration.enderio;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Pure flood-fill over Ender IO's own conduit bundle connectivity. Ender IO
 * does publish a genuine public network API
 * ({@code com.enderio.enderio.api.conduits.network.ConduitNetwork}), but
 * this mod deliberately carries zero compile-time or runtime dependency on
 * Ender IO at all (see {@link EnderIOIntegration}'s class Javadoc), so this
 * reconstructs network membership itself instead of adding that dependency -
 * walking 6-directionally adjacent conduit bundle positions, identified
 * purely by {@link BlockEntityType} identity via the same "known registry
 * name" principle already used throughout this integration. Mirrors exactly
 * how the now-removed {@code SteamNetworkDiscovery} walked this mod's own
 * pipes. Knows nothing about ticks, config, caching, or climate zones;
 * performs no world mutation.
 */
final class EnderIONetworkDiscovery {

    private EnderIONetworkDiscovery() {
    }

    /**
     * @param conduits every position that is part of the discovered network
     *                 (empty if {@code start} was not itself a conduit)
     * @param boundary every position adjacent to the discovered conduit
     *                 network that is not itself part of that network
     *                 (candidate source/sink positions)
     */
    record NetworkResult(Set<BlockPos> conduits, Set<BlockPos> boundary) {
    }

    /**
     * @param start position to begin the flood-fill from; must itself be a
     *              conduit bundle block entity or the result is empty
     * @param conduitType the Ender IO conduit bundle {@link BlockEntityType}
     *                     that counts as "the same network"
     * @return every position adjacent to the discovered conduit network that
     *         is not itself part of that network (candidate source/sink
     *         positions)
     */
    static Set<BlockPos> discoverBoundary(BlockPos start, BlockGetter world, BlockEntityType<?> conduitType) {
        return discover(start, world, conduitType).boundary();
    }

    /**
     * Full flood-fill result: both the discovered network's own membership
     * (used to derive a stable, start-position-independent network identity -
     * see {@link EnderIONetworkPosition#getNetworkId()}) and its boundary.
     * Since the flood-fill discovers the entire connected component
     * regardless of which conduit in it the fill started from, two calls
     * starting from different positions on the same physical network return
     * an equal {@link NetworkResult#conduits()} set.
     *
     * @param start position to begin the flood-fill from; must itself be a
     *              conduit bundle block entity or the result is empty
     * @param conduitType the Ender IO conduit bundle {@link BlockEntityType}
     *                     that counts as "the same network"
     */
    static NetworkResult discover(BlockPos start, BlockGetter world, BlockEntityType<?> conduitType) {
        Set<BlockPos> conduits = new HashSet<>();
        Set<BlockPos> boundary = new HashSet<>();

        BlockEntity startEntity = world.getBlockEntity(start);
        if (startEntity == null || startEntity.getType() != conduitType) {
            return new NetworkResult(conduits, boundary);
        }

        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos startPos = start.immutable();
        queue.add(startPos);
        conduits.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = pos.relative(direction).immutable();
                BlockEntity neighbor = world.getBlockEntity(neighborPos);
                if (neighbor == null) {
                    continue;
                }
                if (neighbor.getType() == conduitType) {
                    if (conduits.add(neighborPos)) {
                        queue.add(neighborPos);
                    }
                } else {
                    boundary.add(neighborPos);
                }
            }
        }

        return new NetworkResult(conduits, boundary);
    }
}
