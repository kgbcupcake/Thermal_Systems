package com.marie.thermalsystems.integration.pneumaticcraft;

import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.zone.ZoneSnapshot;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.desht.pneumaticcraft.api.PNCCapabilities;
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

import java.util.List;
import java.util.Optional;

/**
 * Optional integration with PneumaticCraft: Repressurized. Only ever loaded
 * and initialized when PNC:R is present - see the {@code ModList.isLoaded}
 * guard around {@link #init(IEventBus)} in
 * {@link com.marie.thermalsystems.ThermalSystemsMod}. PNC:R imports exist
 * only within this package; nothing outside {@code integration/pneumaticcraft/}
 * may reference them.
 *
 * <p>This registers this mod's {@code HEAT_SOURCE}/{@code COOLING_SOURCE}
 * capabilities directly onto PNC:R's own, unmodified block entities - it
 * introduces no block, item, or block entity type of its own. Reading
 * PNC:R's heat state goes through PNC:R's own capability
 * ({@link PNCCapabilities#HEAT_EXCHANGER_BLOCK}), never through PNC:R's
 * internal classes.
 *
 * <p>Verified against the PNC:R 8.2.20+mc1.21.1 jar (compileOnly dependency,
 * full mod jar rather than an api-only artifact): {@code PNCCapabilities}
 * declares {@code HEAT_EXCHANGER_BLOCK} as a
 * {@code BlockCapability<IHeatExchangerLogic, Direction>} - a genuine
 * NeoForge-idiomatic sided {@code BlockCapability}, exactly as already used
 * elsewhere in this mod. PNC:R's own {@code CapabilitySetup} registers this
 * capability generically on every block entity implementing its internal
 * {@code IHeatExchangingTE} marker interface; grepping the jar for that
 * interface plus the concrete block entities registered against it confirms
 * the real, complete list of machines that expose it: {@code heat_sink}
 * ({@code HeatSinkBlockEntity}, via its {@code CompressedIronBlockEntity}
 * superclass), {@code refinery} ({@code RefineryControllerBlockEntity}), and
 * {@code thermopneumatic_processing_plant} ({@code ThermoPlantBlockEntity}).
 * As with {@link com.marie.thermalsystems.integration.mekanism.MekanismIntegration},
 * these {@link BlockEntityType}s are looked up at
 * {@link RegisterCapabilitiesEvent} time by their known, documented registry
 * names via {@link BuiltInRegistries#BLOCK_ENTITY_TYPE} rather than
 * importing PNC:R's internal block entity classes, so this stays resilient
 * to PNC:R's internal package being renamed or restructured.
 */
public final class PneumaticCraftIntegration {

    public static final String PNC_MOD_ID = "pneumaticcraft";

    /**
     * Registry names (under the {@code pneumaticcraft} namespace) of PNC:R's
     * own {@link BlockEntityType}s confirmed to implement
     * {@code IHeatExchangingTE} and thus expose
     * {@link PNCCapabilities#HEAT_EXCHANGER_BLOCK}. See the class Javadoc for
     * how this list was verified.
     */
    private static final List<String> HEAT_EXCHANGER_BLOCK_ENTITIES = List.of(
            "heat_sink",
            "refinery",
            "thermopneumatic_processing_plant");

    private PneumaticCraftIntegration() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(RegisterCapabilitiesEvent.class, PneumaticCraftIntegration::onRegisterCapabilities);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, PneumaticCraftIntegration::onRegisterCommands);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        for (String path : HEAT_EXCHANGER_BLOCK_ENTITIES) {
            BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(
                    ResourceLocation.fromNamespaceAndPath(PNC_MOD_ID, path));
            registerHeatAndCooling(event, type);
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerHeatAndCooling(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        BlockEntityType<BlockEntity> blockEntityType = (BlockEntityType<BlockEntity>) type;
        event.registerBlockEntity(HeatSourceCapabilities.HEAT_SOURCE, blockEntityType,
                (blockEntity, context) -> new PneumaticCraftBlockHeatSource(blockEntity.getLevel(), blockEntity.getBlockPos()));
        event.registerBlockEntity(CoolingSourceCapabilities.COOLING_SOURCE, blockEntityType,
                (blockEntity, context) -> new PneumaticCraftBlockHeatSource(blockEntity.getLevel(), blockEntity.getBlockPos()));
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("thermal")
                        .then(Commands.literal("pneumaticcraft")
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
        if (targeted.isEmpty() || HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, targeted.get(), null, null, null) == null) {
            source.sendFailure(Component.literal("You are not looking at a PneumaticCraft heat-capable block."));
            return 0;
        }
        BlockPos pos = targeted.get();

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

        source.sendSuccess(() -> Component.literal("Bound PneumaticCraft block to zone '" + zoneName + "'."), true);
        return 1;
    }

    private static int unbind(CommandSourceStack source) throws CommandSyntaxException {
        Optional<BlockPos> targeted = lookedAtBlock(source.getPlayerOrException());
        Level level = source.getLevel();
        if (targeted.isEmpty() || HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, targeted.get(), null, null, null) == null) {
            source.sendFailure(Component.literal("You are not looking at a PneumaticCraft heat-capable block."));
            return 0;
        }
        BlockPos pos = targeted.get();

        ThermalSystemsAPI.unbindHeatSource(level, pos);
        ThermalSystemsAPI.unbindCoolingSource(level, pos);

        source.sendSuccess(() -> Component.literal("Unbound PneumaticCraft block from its zone."), true);
        return 1;
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
