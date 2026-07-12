package com.marie.thermalsystems.integration.pneumaticcraft;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.zone.ZoneSnapshot;
import com.marie.thermalsystems.integration.pneumaticcraft.block.ThermalExchangerBlock;
import com.marie.thermalsystems.integration.pneumaticcraft.blockentity.ThermalExchangerBlockEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/**
 * Optional integration with PneumaticCraft: Repressurized. Only ever loaded
 * and initialized when PNC:R is present - see the {@code ModList.isLoaded}
 * guard around {@link #init(IEventBus)} in {@link ThermalSystemsMod}. PNC:R
 * imports exist only within this package; nothing outside
 * {@code integration/pneumaticcraft/} may reference them.
 */
public final class PneumaticCraftIntegration {

    public static final String PNC_MOD_ID = "pneumaticcraft";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThermalSystemsMod.MOD_ID);
    public static final DeferredBlock<ThermalExchangerBlock> THERMAL_EXCHANGER = BLOCKS.register("thermal_exchanger",
            () -> new ThermalExchangerBlock(BlockBehaviour.Properties.of().strength(3.0f)));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThermalSystemsMod.MOD_ID);
    public static final DeferredItem<BlockItem> THERMAL_EXCHANGER_ITEM =
            ITEMS.registerSimpleBlockItem("thermal_exchanger", THERMAL_EXCHANGER);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThermalSystemsMod.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermalExchangerBlockEntity>> THERMAL_EXCHANGER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("thermal_exchanger",
                    () -> BlockEntityType.Builder.of(ThermalExchangerBlockEntity::new, THERMAL_EXCHANGER.get()).build(null));

    private PneumaticCraftIntegration() {
    }

    public static void init(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(RegisterCapabilitiesEvent.class, PneumaticCraftIntegration::onRegisterCapabilities);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, PneumaticCraftIntegration::onRegisterCommands);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(HeatSourceCapabilities.HEAT_SOURCE, THERMAL_EXCHANGER_BLOCK_ENTITY.get(),
                (exchanger, context) -> exchanger);
        event.registerBlockEntity(CoolingSourceCapabilities.COOLING_SOURCE, THERMAL_EXCHANGER_BLOCK_ENTITY.get(),
                (exchanger, context) -> exchanger);
        event.registerBlockEntity(PNCCapabilities.HEAT_EXCHANGER_BLOCK, THERMAL_EXCHANGER_BLOCK_ENTITY.get(),
                (exchanger, direction) -> exchanger.getHeatExchangerLogic());
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("thermal")
                        .then(Commands.literal("exchanger")
                                .then(Commands.literal("bind")
                                        .then(Commands.argument("zoneName", StringArgumentType.word())
                                                .executes(context -> bindExchanger(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "zoneName")))))
                                .then(Commands.literal("unbind")
                                        .executes(context -> unbindExchanger(context.getSource())))));
    }

    private static int bindExchanger(CommandSourceStack source, String zoneName) throws CommandSyntaxException {
        Optional<ThermalExchangerBlockEntity> exchanger = lookedAtExchanger(source);
        if (exchanger.isEmpty()) {
            source.sendFailure(Component.literal("You must be looking at a Thermal Exchanger."));
            return 0;
        }

        Level level = source.getLevel();
        Optional<ZoneSnapshot> zone = ThermalSystemsAPI.getZoneByName(level, zoneName);
        if (zone.isEmpty()) {
            source.sendFailure(Component.literal("No climate zone named '" + zoneName + "' exists."));
            return 0;
        }

        BlockPos pos = exchanger.get().getBlockPos();
        try {
            ThermalSystemsAPI.bindHeatSource(level, pos, zone.get().id());
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
        try {
            ThermalSystemsAPI.bindCoolingSource(level, pos, zone.get().id());
        } catch (IllegalArgumentException e) {
            ThermalSystemsAPI.unbindHeatSource(level, pos);
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Bound Thermal Exchanger to zone '" + zoneName + "'."), true);
        return 1;
    }

    private static int unbindExchanger(CommandSourceStack source) throws CommandSyntaxException {
        Optional<ThermalExchangerBlockEntity> exchanger = lookedAtExchanger(source);
        if (exchanger.isEmpty()) {
            source.sendFailure(Component.literal("You must be looking at a Thermal Exchanger."));
            return 0;
        }

        Level level = source.getLevel();
        BlockPos pos = exchanger.get().getBlockPos();
        ThermalSystemsAPI.unbindHeatSource(level, pos);
        ThermalSystemsAPI.unbindCoolingSource(level, pos);

        source.sendSuccess(() -> Component.literal("Unbound Thermal Exchanger from its zone."), true);
        return 1;
    }

    private static Optional<ThermalExchangerBlockEntity> lookedAtExchanger(CommandSourceStack source) throws CommandSyntaxException {
        Optional<BlockPos> targeted = lookedAtBlock(source.getPlayerOrException());
        return targeted
                .map(pos -> source.getLevel().getBlockEntity(pos))
                .filter(ThermalExchangerBlockEntity.class::isInstance)
                .map(ThermalExchangerBlockEntity.class::cast);
    }

    private static Optional<BlockPos> lookedAtBlock(ServerPlayer player) {
        double reach = player.blockInteractionRange();
        Vec3 eyePos = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(viewVector.scale(reach));
        ClipContext clipContext = new ClipContext(
                eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult hitResult = player.level().clip(clipContext);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        return Optional.of(hitResult.getBlockPos());
    }
}
