package com.marie.thermalsystems.integration.mekanism;

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
 * Pure flood-fill over Mekanism's own thermodynamic conductor (cable)
 * connectivity. Mekanism's real network object
 * ({@code mekanism.common.content.network.HeatNetwork}) lives entirely in
 * its internal {@code common} package, not the public {@code :api} jar this
 * mod compiles against, so this reconstructs network membership itself by
 * walking 6-directionally adjacent cable positions - identified purely by
 * {@link BlockEntityType} identity, the same "known registry name" principle
 * used by {@link MekanismIntegration} - exactly mirroring how the
 * now-removed {@code SteamNetworkDiscovery} walked this mod's own pipes.
 * Knows nothing about ticks, config, caching, or climate zones; performs no
 * world mutation.
 */
final class MekanismNetworkDiscovery {

    private MekanismNetworkDiscovery() {
    }

    /**
     * @param start position to begin the flood-fill from; must itself be one
     *              of {@code cableTypes} or the result is empty
     * @param cableTypes the set of Mekanism transmitter {@link BlockEntityType}s
     *                    that count as "the same network"
     * @return every position adjacent to the discovered cable network that is
     *         not itself part of that network (candidate source/sink positions)
     */
    static Set<BlockPos> discoverBoundary(BlockPos start, BlockGetter world, Set<BlockEntityType<?>> cableTypes) {
        Set<BlockPos> cables = new HashSet<>();
        Set<BlockPos> boundary = new HashSet<>();

        BlockEntity startEntity = world.getBlockEntity(start);
        if (startEntity == null || !cableTypes.contains(startEntity.getType())) {
            return boundary;
        }

        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos startPos = start.immutable();
        queue.add(startPos);
        cables.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = pos.relative(direction).immutable();
                BlockEntity neighbor = world.getBlockEntity(neighborPos);
                if (neighbor == null) {
                    continue;
                }
                if (cableTypes.contains(neighbor.getType())) {
                    if (cables.add(neighborPos)) {
                        queue.add(neighborPos);
                    }
                } else {
                    boundary.add(neighborPos);
                }
            }
        }

        return boundary;
    }
}
