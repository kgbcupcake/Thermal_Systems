package com.marie.thermalsystems.registry;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.blockentity.BoilerBlockEntity;
import com.marie.thermalsystems.blockentity.RadiatorBlockEntity;
import com.marie.thermalsystems.blockentity.SteamPipeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers block entity types for Phase 2's steam infrastructure blocks.
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThermalSystemsMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerBlockEntity>> BOILER =
            BLOCK_ENTITY_TYPES.register("boiler",
                    () -> BlockEntityType.Builder.of(BoilerBlockEntity::new, ModBlocks.BOILER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamPipeBlockEntity>> STEAM_PIPE =
            BLOCK_ENTITY_TYPES.register("steam_pipe",
                    () -> BlockEntityType.Builder.of(SteamPipeBlockEntity::new, ModBlocks.STEAM_PIPE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadiatorBlockEntity>> RADIATOR =
            BLOCK_ENTITY_TYPES.register("radiator",
                    () -> BlockEntityType.Builder.of(RadiatorBlockEntity::new, ModBlocks.RADIATOR.get()).build(null));

    private ModBlockEntities() {
    }
}
