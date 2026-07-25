package com.marie.thermalsystems.integration.mekanism;

import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.zone.ZoneSnapshot;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mekanism.api.heat.IHeatHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Optional integration with Mekanism. Only ever loaded and initialized when
 * Mekanism is present - see the {@code ModList.isLoaded} guard around
 * {@link #init(IEventBus)} in {@link com.marie.thermalsystems.ThermalSystemsMod}.
 * Mekanism imports exist only within this package; nothing outside
 * {@code integration/mekanism/} may reference them.
 *
 * <p>Zones no longer bind directly to Mekanism source machines. A Zone binds
 * to a point on a Mekanism thermodynamic conductor (cable) network instead,
 * via {@link MekanismNetworkPosition}, which sums {@code getHeatOutput()}/
 * {@code getCoolingOutput()} across every machine reachable on that network
 * that already exposes this mod's own {@code HEAT_SOURCE}/{@code COOLING_SOURCE}
 * capability - the same registration in {@link #HEAT_CAPABLE_BLOCK_ENTITIES}
 * below, unchanged, still used directly for Jade tooltips on the machines
 * themselves.
 *
 * <p>Mekanism's real network object
 * ({@code mekanism.common.content.network.HeatNetwork}, built on
 * {@code mekanism.common.lib.transmitter.DynamicNetwork}) lives entirely in
 * its internal {@code common} package - confirmed absent from the public
 * {@code :api} jar this mod compiles against (every package in that jar was
 * enumerated; none relate to networks or transmitters). Rather than depend
 * on those internal, unversioned classes, {@link MekanismNetworkDiscovery}
 * reconstructs network membership itself via a 6-directional flood-fill
 * across Mekanism's own thermodynamic conductor block entities - identified
 * purely by the same "known registry name via {@link BuiltInRegistries}"
 * principle already used for {@link #HEAT_CAPABLE_BLOCK_ENTITIES} below, and
 * mirroring exactly how the now-removed {@code SteamNetworkDiscovery} walked
 * this mod's own pipes. This never depends on Mekanism's internal network
 * classes and stays resilient to their internal refactoring.
 *
 * <p>Verified against the Mekanism 1.21.x source (mekanism/Mekanism), full
 * jar (not just {@code :api}): the four thermodynamic conductor tiers -
 * {@code basic_thermodynamic_conductor}, {@code advanced_thermodynamic_conductor},
 * {@code elite_thermodynamic_conductor}, {@code ultimate_thermodynamic_conductor} -
 * are Mekanism's only heat-carrying cables (backed by
 * {@code mekanism.common.tile.transmitter.TileEntityThermodynamicConductor}),
 * confirmed via their blockstate/registry files. See {@link #HEAT_HANDLER}'s
 * javadoc for how the existing {@code IHeatHandler} capability token below
 * was independently verified.
 */
public final class MekanismIntegration {

    public static final String MEKANISM_MOD_ID = "mekanism";

    public static final BlockCapability<IHeatHandler, @Nullable Direction> HEAT_HANDLER = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(MEKANISM_MOD_ID, "heat_handler"), IHeatHandler.class);

    /**
     * Registry names (under the {@code mekanism} namespace) of Mekanism's own
     * {@link BlockEntityType}s confirmed to expose non-empty heat capacitors.
     * See the class Javadoc for how this list was verified.
     */
    private static final List<String> HEAT_CAPABLE_BLOCK_ENTITIES = List.of(
            "boiler_casing",
            "fuelwood_heater",
            "resistive_heater",
            "thermal_evaporation_controller",
            "thermal_evaporation_valve");

    /**
     * Registry names (under the {@code mekanism} namespace) of Mekanism's own
     * heat-carrying transmitter (cable) {@link BlockEntityType}s. A Zone
     * binds to one of these, not to {@link #HEAT_CAPABLE_BLOCK_ENTITIES}
     * directly. See the class Javadoc for how this list was verified.
     */
    private static final List<String> TRANSMITTER_BLOCK_ENTITIES = List.of(
            "basic_thermodynamic_conductor",
            "advanced_thermodynamic_conductor",
            "elite_thermodynamic_conductor",
            "ultimate_thermodynamic_conductor");

    /**
     * Resolved {@link #TRANSMITTER_BLOCK_ENTITIES} types, populated once at
     * {@link RegisterCapabilitiesEvent} time and read by
     * {@link MekanismNetworkPosition} for flood-fill and by the bind/unbind
     * commands for source-vs-network validation.
     */
    static final Set<BlockEntityType<?>> TRANSMITTER_BLOCK_ENTITY_TYPES = new HashSet<>();

    private static final Set<BlockEntityType<?>> HEAT_CAPABLE_BLOCK_ENTITY_TYPES = new HashSet<>();

    private MekanismIntegration() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(RegisterCapabilitiesEvent.class, MekanismIntegration::onRegisterCapabilities);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, MekanismIntegration::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, MekanismIntegration::onServerStopping);
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        MekanismNetworkPosition.clearCache();
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        for (String path : HEAT_CAPABLE_BLOCK_ENTITIES) {
            BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(
                    ResourceLocation.fromNamespaceAndPath(MEKANISM_MOD_ID, path));
            HEAT_CAPABLE_BLOCK_ENTITY_TYPES.add(type);
            registerSourceHeatAndCooling(event, type);
        }
        for (String path : TRANSMITTER_BLOCK_ENTITIES) {
            BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(
                    ResourceLocation.fromNamespaceAndPath(MEKANISM_MOD_ID, path));
            TRANSMITTER_BLOCK_ENTITY_TYPES.add(type);
            registerNetworkHeatAndCooling(event, type);
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerSourceHeatAndCooling(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        BlockEntityType<BlockEntity> blockEntityType = (BlockEntityType<BlockEntity>) type;
        event.registerBlockEntity(HeatSourceCapabilities.HEAT_SOURCE, blockEntityType,
                (blockEntity, context) -> new MekanismBlockHeatSource(blockEntity.getLevel(), blockEntity.getBlockPos()));
        event.registerBlockEntity(CoolingSourceCapabilities.COOLING_SOURCE, blockEntityType,
                (blockEntity, context) -> new MekanismBlockHeatSource(blockEntity.getLevel(), blockEntity.getBlockPos()));
    }

    @SuppressWarnings("unchecked")
    private static void registerNetworkHeatAndCooling(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        BlockEntityType<BlockEntity> blockEntityType = (BlockEntityType<BlockEntity>) type;
        event.registerBlockEntity(HeatSourceCapabilities.HEAT_SOURCE, blockEntityType,
                (blockEntity, context) -> new MekanismNetworkPosition(blockEntity.getLevel(), blockEntity.getBlockPos()));
        event.registerBlockEntity(CoolingSourceCapabilities.COOLING_SOURCE, blockEntityType,
                (blockEntity, context) -> new MekanismNetworkPosition(blockEntity.getLevel(), blockEntity.getBlockPos()));
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("thermal")
                        .then(Commands.literal("mekanism")
                                .then(Commands.literal("bind")
                                        .then(Commands.argument("zoneName", StringArgumentType.word())
                                                .executes(context -> bind(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "zoneName")))))
                                .then(Commands.literal("unbind")
                                        .executes(context -> unbind(context.getSource())))));
    }

    private static int bind(CommandSourceStack source, String zoneName) throws CommandSyntaxException {
        Optional<BlockPos> targeted = lookedAtBlock(source.getPlayerOrException());
        Level level = source.getLevel();
        if (targeted.isEmpty()) {
            source.sendFailure(Component.literal("You are not looking at a block."));
            return 0;
        }
        BlockPos pos = targeted.get();

        if (isSourceBlock(level, pos) && !isNetworkBlock(level, pos)) {
            source.sendFailure(Component.literal("Bind a cable connected to this generator, not the generator itself."));
            return 0;
        }
        if (!isNetworkBlock(level, pos)) {
            source.sendFailure(Component.literal("You are not looking at a Mekanism thermodynamic conductor."));
            return 0;
        }

        Optional<ZoneSnapshot> zone = ThermalSystemsAPI.getZoneByName(level, zoneName);
        if (zone.isEmpty()) {
            source.sendFailure(Component.literal("No climate zone named '" + zoneName + "' exists."));
            return 0;
        }

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

        source.sendSuccess(() -> Component.literal("Bound Mekanism cable to zone '" + zoneName + "'."), true);
        return 1;
    }

    private static int unbind(CommandSourceStack source) throws CommandSyntaxException {
        Optional<BlockPos> targeted = lookedAtBlock(source.getPlayerOrException());
        Level level = source.getLevel();
        if (targeted.isEmpty() || !isNetworkBlock(level, targeted.get())) {
            source.sendFailure(Component.literal("You are not looking at a Mekanism thermodynamic conductor."));
            return 0;
        }
        BlockPos pos = targeted.get();

        ThermalSystemsAPI.unbindHeatSource(level, pos);
        ThermalSystemsAPI.unbindCoolingSource(level, pos);

        source.sendSuccess(() -> Component.literal("Unbound Mekanism cable from its zone."), true);
        return 1;
    }

    private static boolean isNetworkBlock(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && TRANSMITTER_BLOCK_ENTITY_TYPES.contains(blockEntity.getType());
    }

    private static boolean isSourceBlock(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && HEAT_CAPABLE_BLOCK_ENTITY_TYPES.contains(blockEntity.getType());
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
