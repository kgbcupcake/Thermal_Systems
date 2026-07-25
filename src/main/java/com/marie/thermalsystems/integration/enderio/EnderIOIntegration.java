package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.zone.ZoneSnapshot;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.Optional;

/**
 * Optional integration with Ender IO. Only ever loaded and initialized when
 * Ender IO is present - see the {@code ModList.isLoaded} guard around
 * {@link #init(IEventBus)} in {@link com.marie.thermalsystems.ThermalSystemsMod}.
 * Ender IO imports exist only within this package; nothing outside
 * {@code integration/enderio/} may reference them.
 *
 * <p>Zones no longer bind directly to the Stirling Generator. A Zone binds
 * to a point on an Ender IO conduit bundle network instead, via
 * {@link EnderIONetworkPosition}, which sums {@code getHeatOutput()} across
 * every machine reachable on that network that already exposes this mod's
 * own {@code HEAT_SOURCE} capability - the same registration in
 * {@link #onRegisterCapabilities} that adapts the Stirling Generator itself
 * via {@link EnderIOBlockHeatSource}, unchanged, still used directly for
 * Jade tooltips on the generator itself.
 *
 * <p>Ender IO does publish a genuine, stable public network API for exactly
 * this purpose ({@code com.enderio.enderio.api.conduits.network.ConduitNetwork},
 * confirmed via the Team-EnderIO/EnderIO GitHub source, branch {@code 1.21.1},
 * matching this mod's {@code enderio_version}) - unlike Mekanism, whose
 * equivalent network object is confirmed absent from its public {@code :api}
 * jar (see {@code MekanismIntegration}'s class Javadoc), making a flood-fill
 * the only option there. This integration deliberately does not depend on
 * Ender IO's public API either, so that both integrations reconstruct
 * network membership the same way for consistency (an explicit prior
 * decision) and so this mod preserves the zero-compile/runtime-dependency
 * design already in place here (see below) - instead,
 * {@link EnderIONetworkDiscovery} reconstructs network membership itself via
 * a 6-directional flood-fill across Ender IO's own conduit bundle block
 * entities, identified purely by the same "known registry name via
 * {@link BuiltInRegistries}" principle already used below, mirroring exactly
 * how the now-removed {@code SteamNetworkDiscovery} walked this mod's own
 * pipes and how {@code MekanismNetworkDiscovery} walks Mekanism's cables.
 *
 * <p>The Ender IO 8.2.11-beta jar was used during development to manually
 * verify the registry name and capability wiring below by disassembling
 * its classes; unlike the Mekanism/PneumaticCraft integrations, which
 * {@code compileOnly} against those mods' real published API jars, this
 * mod has no compile-time or runtime dependency on Ender IO at all - the
 * findings were simply transcribed as documented registry-name/capability
 * lookups here. The Stirling Generator
 * ({@code enderio:stirling_generator}, backed by
 * {@code com.enderio.enderio.content.machines.stirling_generator.StirlingGeneratorBlockEntity})
 * is the only coal/solid-fuel-fed generator block this release ships - it
 * burns any vanilla furnace fuel via {@code ItemStack.getBurnTime} into FE,
 * making it the correct "coal-fed Heat Generator" target rather than any
 * more speculative block. Disassembling
 * {@code EIOBlockEntities.poweredMachineBlockEntityCapabilities} confirms
 * every {@code PoweredMachineBlockEntity} subtype - including the Stirling
 * Generator - has the standard {@code Capabilities.EnergyStorage.BLOCK}
 * capability registered against it by Ender IO itself, backed by
 * {@code PoweredMachineBlockEntity.ENERGY_STORAGE_PROVIDER}: an ordinary
 * NeoForge {@code IEnergyStorage}, not an Ender-IO-specific type. All of
 * Ender IO's conduit types (energy, fluid, item, redstone) are visually and
 * structurally bundled into a single block/block entity - confirmed via
 * {@code com.enderio.enderio.content.conduits.bundle.ConduitBundleBlockEntity},
 * registered under the single registry name {@code enderio:conduit} - so
 * unlike Mekanism's four separate transmitter tiers, only one
 * {@link BlockEntityType} needs registering here. The Stirling Generator
 * only ever produces energy - it has no cooling/heat-sink analog - so only
 * {@code HEAT_SOURCE} is registered here, unlike the Mekanism/PneumaticCraft
 * integrations which also register {@code COOLING_SOURCE}.
 */
public final class EnderIOIntegration {

    public static final String ENDERIO_MOD_ID = "enderio";

    private static final String STIRLING_GENERATOR_PATH = "stirling_generator";
    private static final String CONDUIT_PATH = "conduit";

    private static BlockEntityType<?> stirlingGeneratorType;

    /**
     * Resolved conduit bundle {@link BlockEntityType}, populated once at
     * {@link RegisterCapabilitiesEvent} time and read by
     * {@link EnderIONetworkPosition} for flood-fill and by the bind/unbind
     * commands for source-vs-network validation.
     */
    static BlockEntityType<?> CONDUIT_BLOCK_ENTITY_TYPE;

    private EnderIOIntegration() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(RegisterCapabilitiesEvent.class, EnderIOIntegration::onRegisterCapabilities);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, EnderIOIntegration::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, EnderIOIntegration::onServerStopping);
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        EnderIONetworkPosition.clearCache();
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        BlockEntityType<?> generatorType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(
                ResourceLocation.fromNamespaceAndPath(ENDERIO_MOD_ID, STIRLING_GENERATOR_PATH));
        stirlingGeneratorType = generatorType;
        registerSourceHeat(event, generatorType);

        BlockEntityType<?> conduitType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(
                ResourceLocation.fromNamespaceAndPath(ENDERIO_MOD_ID, CONDUIT_PATH));
        CONDUIT_BLOCK_ENTITY_TYPE = conduitType;
        registerNetworkHeat(event, conduitType);
    }

    @SuppressWarnings("unchecked")
    private static void registerSourceHeat(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        BlockEntityType<BlockEntity> blockEntityType = (BlockEntityType<BlockEntity>) type;
        event.registerBlockEntity(HeatSourceCapabilities.HEAT_SOURCE, blockEntityType,
                (blockEntity, context) -> new EnderIOBlockHeatSource(blockEntity.getLevel(), blockEntity.getBlockPos()));
    }

    @SuppressWarnings("unchecked")
    private static void registerNetworkHeat(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        BlockEntityType<BlockEntity> blockEntityType = (BlockEntityType<BlockEntity>) type;
        event.registerBlockEntity(HeatSourceCapabilities.HEAT_SOURCE, blockEntityType,
                (blockEntity, context) -> new EnderIONetworkPosition(blockEntity.getLevel(), blockEntity.getBlockPos()));
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("thermal")
                        .then(Commands.literal("enderio")
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
            source.sendFailure(Component.literal("Bind a conduit connected to this generator, not the generator itself."));
            return 0;
        }
        if (!isNetworkBlock(level, pos)) {
            source.sendFailure(Component.literal("You are not looking at an Ender IO conduit."));
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

        source.sendSuccess(() -> Component.literal("Bound Ender IO conduit to zone '" + zoneName + "'."), true);
        return 1;
    }

    private static int unbind(CommandSourceStack source) throws CommandSyntaxException {
        Optional<BlockPos> targeted = lookedAtBlock(source.getPlayerOrException());
        Level level = source.getLevel();
        if (targeted.isEmpty() || !isNetworkBlock(level, targeted.get())) {
            source.sendFailure(Component.literal("You are not looking at an Ender IO conduit."));
            return 0;
        }
        BlockPos pos = targeted.get();

        ThermalSystemsAPI.unbindHeatSource(level, pos);

        source.sendSuccess(() -> Component.literal("Unbound Ender IO conduit from its zone."), true);
        return 1;
    }

    private static boolean isNetworkBlock(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && blockEntity.getType() == CONDUIT_BLOCK_ENTITY_TYPE;
    }

    private static boolean isSourceBlock(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && blockEntity.getType() == stirlingGeneratorType;
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
