package com.marie.thermalsystems.steam;

import com.marie.thermalsystems.blockentity.BoilerBlockEntity;
import com.marie.thermalsystems.blockentity.RadiatorBlockEntity;
import com.marie.thermalsystems.blockentity.SteamPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Pure flood-fill over steam pipe connectivity. Given a starting position,
 * walks 6-directionally adjacent SteamPipe positions and collects every
 * BoilerBlockEntity and RadiatorBlockEntity directly adjacent to any pipe in
 * the resulting component. Knows nothing about dirty state, ticks, config,
 * logging, or climate zones; performs no world mutation.
 */
public final class SteamNetworkDiscovery {

    private SteamNetworkDiscovery() {
    }

    public static SteamNetwork discover(BlockPos start, BlockGetter world) {
        Set<BlockPos> pipes = new HashSet<>();
        Set<BlockPos> boilers = new HashSet<>();
        Set<BlockPos> radiators = new HashSet<>();

        if (!(world.getBlockEntity(start) instanceof SteamPipeBlockEntity)) {
            return new SteamNetwork(pipes, boilers, radiators);
        }

        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos startPos = start.immutable();
        queue.add(startPos);
        pipes.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = pos.relative(direction).immutable();
                BlockEntity neighbor = world.getBlockEntity(neighborPos);
                if (neighbor instanceof SteamPipeBlockEntity) {
                    if (pipes.add(neighborPos)) {
                        queue.add(neighborPos);
                    }
                } else if (neighbor instanceof BoilerBlockEntity) {
                    boilers.add(neighborPos);
                } else if (neighbor instanceof RadiatorBlockEntity) {
                    radiators.add(neighborPos);
                }
            }
        }

        return new SteamNetwork(pipes, boilers, radiators);
    }
}
