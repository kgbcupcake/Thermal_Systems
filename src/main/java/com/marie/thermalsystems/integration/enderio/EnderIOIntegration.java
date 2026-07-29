package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.zone.ZoneSnapshot;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.radiation.ActiveSourcePositions;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marie.framework.api.marieapi.MarieAPI;
import dev.marie.framework.network.GenericStateSyncPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

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
 * {@link BlockEntityType} needs registering here. The Stirling Generator has
 * no native cooling/heat-sink analog of its own - unlike Mekanism's
 * temperature-delta-driven heat handlers, converted energy has no inherent
 * heat-or-cool sign - so {@code COOLING_SOURCE} is registered here too, but
 * gated by an explicit, in-memory-only {@link ThermalMode} per generator
 * (see {@link EnderIOAdapterModeRegistry}) rather than derived from any real
 * machine state. That mode defaults to {@code HEAT} and is switched either
 * by the {@link HeatCoolToggleComponent} overlay {@link EnderIOClientIntegration}
 * renders directly on the Stirling Generator's own screen (the primary
 * control surface, wired through {@code MarieAPI.registerGenericStateSyncHandler}
 * to {@link #onGenericStateSync} below) or, as a fallback, via the
 * {@code /thermal enderio setMode} command.
 */
public final class EnderIOIntegration {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnderIOIntegration.class);

    public static final String ENDERIO_MOD_ID = "enderio";

    private static final String STIRLING_GENERATOR_PATH = "stirling_generator";
    private static final String CONDUIT_PATH = "conduit";

    private static int reverifyTickCounter = 0;

    private static BlockEntityType<?> stirlingGeneratorType;

    /**
     * Cached alongside {@link #stirlingGeneratorType} so {@link #onBlockPlaced}/
     * {@link #onBlockBroken} can identify the block from {@link BlockEvent}'s
     * {@link net.minecraft.world.level.block.state.BlockState}, which is
     * always populated at event time regardless of whether the block entity
     * has been attached/detached yet - unlike {@link #isTrackedSourceType},
     * which depends on {@code level.getBlockEntity(pos)} already resolving.
     */
    private static Block stirlingGeneratorBlock;

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
        if (!ThermalConfig.ENDERIO_ENABLED.get()) {
            return;
        }
        modEventBus.addListener(RegisterCapabilitiesEvent.class, EnderIOIntegration::onRegisterCapabilities);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, EnderIOIntegration::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, EnderIOIntegration::onServerStopping);
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Load.class, EnderIOIntegration::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Unload.class, EnderIOIntegration::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(BlockEvent.EntityPlaceEvent.class, EnderIOIntegration::onBlockPlaced);
        NeoForge.EVENT_BUS.addListener(BlockEvent.BreakEvent.class, EnderIOIntegration::onBlockBroken);
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, EnderIOIntegration::onServerTick);
        MarieAPI.registerGenericStateSyncHandler(EnderIOIntegration::onGenericStateSync);
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, EnderIOIntegration::onRegisterPayloadHandlers);
        MarieAPI.registerBlockHoverProvider(new EnderIONetworkHoverProvider());
    }

    /**
     * Registers {@link EnderIOModeRequestPayload} (client-to-server only -
     * the server-to-client reply leg, {@link EnderIOModeResponsePayload}, is
     * registered separately by {@link EnderIOClientIntegration}, which is
     * the only side that ever needs to receive it).
     */
    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ThermalSystemsMod.MOD_ID).versioned("1");
        registrar.playToServer(EnderIOModeRequestPayload.TYPE, EnderIOModeRequestPayload.STREAM_CODEC,
                EnderIOIntegration::onModeRequest);
    }

    /**
     * Replies with the requested position's current {@link ThermalMode} via
     * {@link IPayloadContext#reply}, but only once {@link #isSourceBlock}
     * confirms the position is actually a Stirling Generator - a client
     * could otherwise ask about an arbitrary position.
     */
    private static void onModeRequest(EnderIOModeRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            BlockPos pos = payload.pos();
            if (!isSourceBlock(level, pos)) {
                return;
            }
            ThermalMode mode = EnderIOAdapterModeRegistry.get(level, pos);
            context.reply(new EnderIOModeResponsePayload(pos, mode.name()));
        });
    }

    /**
     * Server-side handler for {@link HeatCoolToggleComponent}'s sync packets
     * - the primary control surface for a generator's {@link ThermalMode},
     * replacing {@code /thermal enderio setMode} (still a valid fallback,
     * see {@link #setMode}). The generic sync channel is shared across every
     * mod that uses it, so {@code payload.pos()} is re-validated against
     * {@link #isSourceBlock} here rather than trusted - a different mod's
     * unrelated use of the same channel must never be mistaken for a mode
     * change.
     */
    private static void onGenericStateSync(ServerPlayer player, GenericStateSyncPayload payload) {
        CompoundTag data = payload.data();
        if (!data.contains("mode")) {
            return;
        }
        Level level = player.level();
        BlockPos pos = payload.pos();
        if (!isSourceBlock(level, pos)) {
            return;
        }
        ThermalMode mode;
        try {
            mode = ThermalMode.valueOf(data.getString("mode"));
        } catch (IllegalArgumentException e) {
            return;
        }
        EnderIOAdapterModeRegistry.set(level, pos, mode);
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        EnderIONetworkPosition.clearCache();
        EnderIOAdapterModeRegistry.clear();
    }

    /**
     * Populates {@link ActiveSourcePositions} for direct-radiation heating
     * (see {@code SourceRadiationTickHandler}), covering sources that already
     * existed in the world before this session started - a freshly loaded
     * world's chunks were never placed this session, so {@link #onBlockPlaced}
     * below can't retroactively cover them. On load, every position in the
     * chunk already holding a block entity is checked against the
     * {@code stirling_generator} type resolved in
     * {@link #onRegisterCapabilities}.
     *
     * <p>Newly placed/broken generators within an already-running session are
     * instead handled immediately and unconditionally by
     * {@link #onBlockPlaced}/{@link #onBlockBroken} - chunk load/unload
     * remains solely responsible for the "already there when the chunk first
     * loads" case.
     *
     * <p>Deliberately does <em>not</em> also track {@code conduit} positions
     * here. {@link EnderIONetworkPosition}, registered against the conduit
     * type, already sums {@code getHeatOutput()} across an entire discovered
     * network - including whichever generators feed it - whenever it is
     * queried from any point on that network. Since a generator's conduits
     * sit directly adjacent to it, tracking both the generator's own
     * position and its conduit positions in the same flat
     * {@link ActiveSourcePositions} set meant
     * {@code SourceRadiationTickHandler} summed the generator's output once
     * directly plus once again per nearby conduit segment - each reading the
     * same whole-network total independently and on its own recompute-cache
     * timer, which is what produced the large, rapidly swinging combined
     * totals reported near a single stationary source. Only the generator
     * position is tracked for direct radiation; {@link EnderIONetworkPosition}
     * remains the network-summing view used exclusively for zone-conduit
     * binding via {@code /thermal enderio bind}, entirely separate from
     * {@link ActiveSourcePositions}.
     */
    private static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ChunkAccess chunk = event.getChunk();
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            if (isTrackedSourceType(level, pos)) {
                ActiveSourcePositions.add(level, pos, "chunk load");
            }
        }
    }

    /**
     * Diagnostic step requested after the third distinct tracking-loss
     * incident traced to this class: unconditionally logs that a
     * {@code ChunkEvent.Unload} actually fired for this chunk, with its
     * coordinates and how many tracked positions it dropped, at the same
     * point in time {@link ActiveSourcePositions#remove} is called below. If
     * a tracked position vanishes from the logs without a matching
     * "onChunkUnload fired" line immediately before it, the eviction is not
     * going through this event at all - some other code path is responsible.
     */
    private static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ChunkAccess chunk = event.getChunk();
        ChunkPos chunkPos = chunk.getPos();
        LOGGER.info("[MTS] EnderIOIntegration onChunkUnload fired dim={} chunk=({},{})",
                level.dimension().location(), chunkPos.x, chunkPos.z);
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            ActiveSourcePositions.remove(level, pos, "chunk unload");
        }
    }

    /**
     * Discovers newly placed generators the instant they're placed, entirely
     * independent of any player's distance from the block - tracking must
     * never be gated on proximity, only radiation delivery
     * ({@code SourceRadiationTickHandler}) should be. This is what closes the
     * same-session-placement gap chunk load/unload alone can't: a generator
     * placed into an already-loaded chunk fires no {@link ChunkEvent.Load}.
     *
     * <p>Only matches {@link #stirlingGeneratorBlock} - conduits are
     * deliberately excluded here too, for the same reason given in
     * {@link #onChunkLoad}'s Javadoc.
     */
    private static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getPlacedBlock().getBlock() != stirlingGeneratorBlock) {
            return;
        }
        ActiveSourcePositions.add(level, event.getPos(), "block placed");
    }

    /**
     * Mirror of {@link #onBlockPlaced} for removal: fires immediately when a
     * tracked generator is broken, regardless of player distance. Uses
     * {@link BlockEvent#getState()} (the state being broken) rather than a
     * block-entity lookup, since {@code BreakEvent} fires before the block is
     * actually removed but there is no guarantee a block-entity lookup at
     * this exact point is any more reliable than the state NeoForge already
     * captured for the event.
     */
    private static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getState().getBlock() != stirlingGeneratorBlock) {
            return;
        }
        ActiveSourcePositions.remove(level, event.getPos(), "block broken");
    }

    /**
     * Periodic supplement to chunk load/unload and the block place/break
     * hooks above: every {@code sourceTrackingReverifyInterval} ticks,
     * re-validates that every currently tracked position still resolves to a
     * {@code stirling_generator} block entity, dropping any that don't. Pure
     * validation of a known, bounded set - discovery of new positions is
     * handled instantly and unconditionally by {@link #onBlockPlaced}, so
     * this no longer needs to scan chunk contents near online players (or
     * depend on player position at all) to catch anything.
     */
    private static void onServerTick(ServerTickEvent.Post event) {
        if (!ThermalConfig.ENDERIO_ENABLED.get() || stirlingGeneratorType == null) {
            return;
        }
        int interval = ThermalConfig.SOURCE_TRACKING_REVERIFY_INTERVAL.get();
        reverifyTickCounter++;
        if (reverifyTickCounter < interval) {
            return;
        }
        reverifyTickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            reverifyTrackedPositionsStillValid(level);
        }
    }

    private static void reverifyTrackedPositionsStillValid(ServerLevel level) {
        Set<BlockPos> tracked = ActiveSourcePositions.getAll(level);
        for (Iterator<BlockPos> iterator = tracked.iterator(); iterator.hasNext(); ) {
            BlockPos pos = iterator.next();
            if (!isTrackedSourceType(level, pos)) {
                iterator.remove();
                if (ThermalConfig.LOGGING_ENABLED.get() && ThermalConfig.RADIATION_LOGGING_ENABLED.get()) {
                    LOGGER.info("[MTS] ActiveSourcePositions remove dim={} pos={} wasPresent=true reason={}",
                            level.dimension().location(), pos, "periodic reverify (no longer a stirling_generator)");
                }
            }
        }
    }

    /**
     * Matches only the generator type - see the {@link #onChunkLoad}
     * Javadoc for why conduit positions must not also be tracked here.
     */
    private static boolean isTrackedSourceType(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }
        return blockEntity.getType() == stirlingGeneratorType;
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        BlockEntityType<?> generatorType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(
                ResourceLocation.fromNamespaceAndPath(ENDERIO_MOD_ID, STIRLING_GENERATOR_PATH));
        stirlingGeneratorType = generatorType;
        stirlingGeneratorBlock = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(ENDERIO_MOD_ID, STIRLING_GENERATOR_PATH));
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
        event.registerBlockEntity(CoolingSourceCapabilities.COOLING_SOURCE, blockEntityType,
                (blockEntity, context) -> new EnderIOBlockHeatSource(blockEntity.getLevel(), blockEntity.getBlockPos()));
    }

    @SuppressWarnings("unchecked")
    private static void registerNetworkHeat(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        BlockEntityType<BlockEntity> blockEntityType = (BlockEntityType<BlockEntity>) type;
        event.registerBlockEntity(HeatSourceCapabilities.HEAT_SOURCE, blockEntityType,
                (blockEntity, context) -> new EnderIONetworkPosition(blockEntity.getLevel(), blockEntity.getBlockPos()));
        event.registerBlockEntity(CoolingSourceCapabilities.COOLING_SOURCE, blockEntityType,
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
                                        .executes(context -> unbind(context.getSource())))
                                .then(Commands.literal("setMode")
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .executes(context -> setMode(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "mode")))))));
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
        try {
            ThermalSystemsAPI.bindCoolingSource(level, pos, zone.get().id());
        } catch (IllegalArgumentException e) {
            ThermalSystemsAPI.unbindHeatSource(level, pos);
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
        ThermalSystemsAPI.unbindCoolingSource(level, pos);

        source.sendSuccess(() -> Component.literal("Unbound Ender IO conduit from its zone."), true);
        return 1;
    }

    /**
     * Fallback control for testing/edge cases: sets the looked-at Stirling
     * Generator's {@link ThermalMode} directly. Superseded by the
     * {@link HeatCoolToggleComponent} overlay {@link EnderIOClientIntegration}
     * renders on the generator's own screen, which is now the primary,
     * player-facing control surface - don't build anything further around
     * this command as though it were the intended interface.
     */
    private static int setMode(CommandSourceStack source, String modeArgument) throws CommandSyntaxException {
        Optional<BlockPos> targeted = lookedAtBlock(source.getPlayerOrException());
        Level level = source.getLevel();
        if (targeted.isEmpty() || !isSourceBlock(level, targeted.get())) {
            source.sendFailure(Component.literal("You are not looking at an Ender IO Stirling Generator."));
            return 0;
        }
        BlockPos pos = targeted.get();

        ThermalMode mode;
        try {
            mode = ThermalMode.valueOf(modeArgument.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Mode must be 'heat' or 'cool'."));
            return 0;
        }

        EnderIOAdapterModeRegistry.set(level, pos, mode);

        source.sendSuccess(() -> Component.literal(
                "Set Ender IO Stirling Generator mode to " + mode.name().toLowerCase(Locale.ROOT) + "."), true);
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
