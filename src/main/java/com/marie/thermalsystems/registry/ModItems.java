package com.marie.thermalsystems.registry;

import com.marie.thermalsystems.ThermalSystemsMod;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers item forms of Phase 2's steam infrastructure blocks.
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThermalSystemsMod.MOD_ID);

    public static final DeferredItem<BlockItem> BOILER = ITEMS.registerSimpleBlockItem("boiler", ModBlocks.BOILER);
    public static final DeferredItem<BlockItem> STEAM_PIPE = ITEMS.registerSimpleBlockItem("steam_pipe", ModBlocks.STEAM_PIPE);
    public static final DeferredItem<BlockItem> RADIATOR = ITEMS.registerSimpleBlockItem("radiator", ModBlocks.RADIATOR);

    private ModItems() {
    }
}
