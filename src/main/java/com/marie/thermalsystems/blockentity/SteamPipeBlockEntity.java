package com.marie.thermalsystems.blockentity;

import com.marie.thermalsystems.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * No state beyond position; exists purely as a connectivity node for
 * {@link com.marie.thermalsystems.steam.SteamNetworkDiscovery}.
 */
public class SteamPipeBlockEntity extends BlockEntity {

    public SteamPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_PIPE.get(), pos, state);
    }
}
