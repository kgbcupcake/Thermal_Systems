package com.marie.thermalsystems.registry;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.block.BoilerBlock;
import com.marie.thermalsystems.block.RadiatorBlock;
import com.marie.thermalsystems.block.SteamPipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers Phase 2's steam infrastructure blocks.
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThermalSystemsMod.MOD_ID);

    public static final DeferredBlock<BoilerBlock> BOILER = BLOCKS.register("boiler",
            () -> new BoilerBlock(BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<SteamPipeBlock> STEAM_PIPE = BLOCKS.register("steam_pipe",
            () -> new SteamPipeBlock(BlockBehaviour.Properties.of().strength(2.0f)));

    public static final DeferredBlock<RadiatorBlock> RADIATOR = BLOCKS.register("radiator",
            () -> new RadiatorBlock(BlockBehaviour.Properties.of().strength(2.5f)));

    private ModBlocks() {
    }
}
