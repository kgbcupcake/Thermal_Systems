package com.marie.thermalsystems.integration.mekanism;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.zone.ZoneSnapshot;
import com.marie.thermalsystems.integration.mekanism.block.MekanismHeatExchangerBlock;
import com.marie.thermalsystems.integration.mekanism.blockentity.MekanismHeatExchangerBlockEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mekanism.api.heat.IHeatHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Optional integration with Mekanism. Only ever loaded and initialized when
 * Mekanism is present - see the {@code ModList.isLoaded} guard around
 * {@link #init(IEventBus)} in {@link ThermalSystemsMod}. Mekanism imports
 * exist only within this package; nothing outside
 * {@code integration/mekanism/} may reference them.
 *
 * <p>Verified against the Mekanism 1.21.x source (mekanism/Mekanism):
 * Mekanism's heat capability is {@code mekanism.api.heat.IHeatHandler}
 * (sided variant {@code IMekanismHeatHandler}), part of the public
 * {@code :api} jar. Temperature is an absolute Kelvin value per capacitor
 * ({@code mekanism.api.heat.HeatAPI.AMBIENT_TEMP == 300}). The capability
 * token itself ({@code mekanism:heat_handler}) is declared in
 * {@code mekanism.common.capabilities.Capabilities}, a class outside the
 * {@code :api} jar this mod compiles against - so {@link #HEAT_HANDLER}
 * below reconstructs the identical token from its public
 * {@link ResourceLocation} and value type, which is how NeoForge capability
 * identity works (registry keyed by location + type, not by which class
 * happened to declare the constant). This adapter never references Ender
 * IO; because Ender IO's own heat conduit relays this same Mekanism
 * capability, it connects to this adapter automatically once both mods are
 * present.
 */
public final class MekanismIntegration {

    public static final String MEKANISM_MOD_ID = "mekanism";

    public static final BlockCapability<IHeatHandler, @Nullable Direction> HEAT_HANDLER = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(MEKANISM_MOD_ID, "heat_handler"), IHeatHandler.class);

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThermalSystemsMod.MOD_ID);
    public static final DeferredBlock<MekanismHeatExchangerBlock> HEAT_EXCHANGER = BLOCKS.register("mekanism_heat_exchanger",
            () -> new MekanismHeatExchangerBlock(BlockBehaviour.Properties.of().strength(3.0f)));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThermalSystemsMod.MOD_ID);
    public static final DeferredItem<BlockItem> HEAT_EXCHANGER_ITEM =
            ITEMS.registerSimpleBlockItem("mekanism_heat_exchanger", HEAT_EXCHANGER);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThermalSystemsMod.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MekanismHeatExchangerBlockEntity>> HEAT_EXCHANGER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("mekanism_heat_exchanger",
                    () -> BlockEntityType.Builder.of(MekanismHeatExchangerBlockEntity::new, HEAT_EXCHANGER.get()).build(null));

    private MekanismIntegration() {
    }

    public static void init(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(RegisterCapabilitiesEvent.class, MekanismIntegration::onRegisterCapabilities);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, MekanismIntegration::onRegisterCommands);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(HeatSourceCapabilities.HEAT_SOURCE, HEAT_EXCHANGER_BLOCK_ENTITY.get(),
                (exchanger, context) -> exchanger);
        event.registerBlockEntity(CoolingSourceCapabilities.COOLING_SOURCE, HEAT_EXCHANGER_BLOCK_ENTITY.get(),
                (exchanger, context) -> exchanger);
        event.registerBlockEntity(HEAT_HANDLER, HEAT_EXCHANGER_BLOCK_ENTITY.get(),
                (exchanger, side) -> exchanger);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("thermal")
                        .then(Commands.literal("mekanism")
                                .then(Commands.literal("bind")
                                        .then(Commands.argument("zoneName", StringArgumentType.word())
                                                .executes(context -> bindExchanger(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "zoneName")))))
                                .then(Commands.literal("unbind")
                                        .executes(context -> unbindExchanger(context.getSource())))));
    }

    private static int bindExchanger(CommandSourceStack source, String zoneName) throws CommandSyntaxException {
        Optional<MekanismHeatExchangerBlockEntity> exchanger = lookedAtExchanger(source);
        if (exchanger.isEmpty()) {
            source.sendFailure(Component.literal("You must be looking at a Mekanism Heat Exchanger."));
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

        source.sendSuccess(() -> Component.literal("Bound Mekanism Heat Exchanger to zone '" + zoneName + "'."), true);
        return 1;
    }

    private static int unbindExchanger(CommandSourceStack source) throws CommandSyntaxException {
        Optional<MekanismHeatExchangerBlockEntity> exchanger = lookedAtExchanger(source);
        if (exchanger.isEmpty()) {
            source.sendFailure(Component.literal("You must be looking at a Mekanism Heat Exchanger."));
            return 0;
        }

        Level level = source.getLevel();
        BlockPos pos = exchanger.get().getBlockPos();
        ThermalSystemsAPI.unbindHeatSource(level, pos);
        ThermalSystemsAPI.unbindCoolingSource(level, pos);

        source.sendSuccess(() -> Component.literal("Unbound Mekanism Heat Exchanger from its zone."), true);
        return 1;
    }

    private static Optional<MekanismHeatExchangerBlockEntity> lookedAtExchanger(CommandSourceStack source) throws CommandSyntaxException {
        Optional<BlockPos> targeted = lookedAtBlock(source.getPlayerOrException());
        return targeted
                .map(pos -> source.getLevel().getBlockEntity(pos))
                .filter(MekanismHeatExchangerBlockEntity.class::isInstance)
                .map(MekanismHeatExchangerBlockEntity.class::cast);
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
