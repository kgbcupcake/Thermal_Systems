package com.marie.thermalsystems.registry;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.block.WallThermostatBlock;
import com.marie.thermalsystems.block.WallThermostatBlockEntity;
import com.marie.thermalsystems.item.CopiedSettings;
import com.marie.thermalsystems.item.GadgetMode;
import com.marie.thermalsystems.item.ThermostatTabletItem;
import com.marie.thermalsystems.item.ZoneGadgetItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

/**
 * Every block, item, block entity type, item data component and creative tab this mod registers:
 * the Zone Gadget, the thermostat tablet, and the wall thermostat the tablet mounts as.
 */
public final class ThermalRegistries {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThermalSystemsMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThermalSystemsMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThermalSystemsMod.MOD_ID);
    private static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ThermalSystemsMod.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThermalSystemsMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> CORNER_ONE =
            DATA_COMPONENTS.registerComponentType("corner_one",
                    builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> CORNER_TWO =
            DATA_COMPONENTS.registerComponentType("corner_two",
                    builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GadgetMode>> GADGET_MODE =
            DATA_COMPONENTS.registerComponentType("gadget_mode",
                    builder -> builder.persistent(GadgetMode.CODEC).networkSynchronized(GadgetMode.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> EDIT_TARGET =
            DATA_COMPONENTS.registerComponentType("edit_target",
                    builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CopiedSettings>> COPIED_SETTINGS =
            DATA_COMPONENTS.registerComponentType("copied_settings",
                    builder -> builder.persistent(CopiedSettings.CODEC).networkSynchronized(CopiedSettings.STREAM_CODEC));

    public static final DeferredBlock<WallThermostatBlock> WALL_THERMOSTAT = BLOCKS.register("wall_thermostat",
            () -> new WallThermostatBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<ZoneGadgetItem> ZONE_GADGET = ITEMS.register("zone_gadget",
            () -> new ZoneGadgetItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ThermostatTabletItem> THERMOSTAT_TABLET = ITEMS.register("thermostat_tablet",
            () -> new ThermostatTabletItem(WALL_THERMOSTAT.get(), new Item.Properties().stacksTo(16)));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WallThermostatBlockEntity>> WALL_THERMOSTAT_ENTITY =
            BLOCK_ENTITY_TYPES.register("wall_thermostat",
                    () -> BlockEntityType.Builder.of(WallThermostatBlockEntity::new, WALL_THERMOSTAT.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.thermalsystems"))
                    .icon(() -> new ItemStack(THERMOSTAT_TABLET.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ZONE_GADGET.get());
                        output.accept(THERMOSTAT_TABLET.get());
                    })
                    .build());

    private ThermalRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
