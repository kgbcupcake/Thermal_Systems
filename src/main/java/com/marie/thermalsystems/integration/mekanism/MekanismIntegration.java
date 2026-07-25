package com.marie.thermalsystems.integration.mekanism;

import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.heating.IHeatSource;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Optional integration with Mekanism. Only ever loaded and initialized when
 * Mekanism is present - see the {@code ModList.isLoaded} guard around
 * {@link #init(IEventBus)} in {@link com.marie.thermalsystems.ThermalSystemsMod}.
 * Mekanism imports exist only within this package; nothing outside
 * {@code integration/mekanism/} may reference them.
 *
 * <p>This registers this mod's {@code HEAT_SOURCE}/{@code COOLING_SOURCE}
 * capabilities directly onto Mekanism's own, unmodified block entities -
 * it introduces no block, item, or block entity type of its own. Reading
 * Mekanism's heat state goes through Mekanism's own capability
 * ({@link #HEAT_HANDLER}), never through Mekanism's internal classes, so this
 * only depends on the public {@code mekanism.api} package this mod already
 * compiles against.
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
 * happened to declare the constant).
 *
 * <p>{@code mekanism.common.registration.impl.TileEntityTypeDeferredRegister}
 * wires {@code Capabilities.HEAT} onto every {@code TileEntityMekanism}
 * generically, but only block entities that actually populate heat
 * capacitors return a non-null handler. Grepping Mekanism's own tile
 * sources for {@code getInitialHeatCapacitors}/{@code HeatCapacitorHelper}
 * usage in the core module (the {@code generators} submodule is a separate
 * mod not covered by this mod's compile-time {@code :api} dependency and is
 * out of scope) confirms the real, complete list of machines below:
 * {@code boiler_casing}, {@code fuelwood_heater}, {@code resistive_heater},
 * {@code thermal_evaporation_controller}, {@code thermal_evaporation_valve}.
 * The {@code mekanism.common} classes that declare and register these
 * {@link BlockEntityType}s live outside the {@code :api} jar, so this looks
 * them up at {@link RegisterCapabilitiesEvent} time by their known,
 * documented registry names via {@link BuiltInRegistries#BLOCK_ENTITY_TYPE}
 * rather than importing Mekanism's internal registry classes - the same
 * "identify by {@link ResourceLocation}, not by internal class" principle
 * used for {@link #HEAT_HANDLER} above. This adapter never references Ender
 * IO; because Ender IO's own heat conduit relays this same Mekanism
 * capability, it connects to these machines automatically once both mods
 * are present.
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

    private MekanismIntegration() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(RegisterCapabilitiesEvent.class, MekanismIntegration::onRegisterCapabilities);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, MekanismIntegration::onRegisterCommands);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        for (String path : HEAT_CAPABLE_BLOCK_ENTITIES) {
            BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(
                    ResourceLocation.fromNamespaceAndPath(MEKANISM_MOD_ID, path));
            registerHeatAndCooling(event, type);
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerHeatAndCooling(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        BlockEntityType<BlockEntity> blockEntityType = (BlockEntityType<BlockEntity>) type;
        event.registerBlockEntity(HeatSourceCapabilities.HEAT_SOURCE, blockEntityType,
                (blockEntity, context) -> new MekanismBlockHeatSource(blockEntity.getLevel(), blockEntity.getBlockPos()));
        event.registerBlockEntity(CoolingSourceCapabilities.COOLING_SOURCE, blockEntityType,
                (blockEntity, context) -> new MekanismBlockHeatSource(blockEntity.getLevel(), blockEntity.getBlockPos()));
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
        if (targeted.isEmpty() || HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, targeted.get(), null, null, null) == null) {
            source.sendFailure(Component.literal("You are not looking at a Mekanism heat-capable block."));
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

        source.sendSuccess(() -> Component.literal("Bound Mekanism block to zone '" + zoneName + "'."), true);
        return 1;
    }

    private static int unbind(CommandSourceStack source) throws CommandSyntaxException {
        Optional<BlockPos> targeted = lookedAtBlock(source.getPlayerOrException());
        Level level = source.getLevel();
        if (targeted.isEmpty() || HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, targeted.get(), null, null, null) == null) {
            source.sendFailure(Component.literal("You are not looking at a Mekanism heat-capable block."));
            return 0;
        }
        BlockPos pos = targeted.get();

        ThermalSystemsAPI.unbindHeatSource(level, pos);
        ThermalSystemsAPI.unbindCoolingSource(level, pos);

        source.sendSuccess(() -> Component.literal("Unbound Mekanism block from its zone."), true);
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
