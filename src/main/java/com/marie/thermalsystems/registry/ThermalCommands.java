package com.marie.thermalsystems.registry;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.blockentity.BoilerBlockEntity;
import com.marie.thermalsystems.blockentity.RadiatorBlockEntity;
import com.marie.thermalsystems.climate.ClimateManager;
import com.marie.thermalsystems.heating.HeatSource;
import com.marie.thermalsystems.zone.ClimateZone;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;

/**
 * Registers the {@code /thermal} command tree. Wires player input into
 * {@link ClimateManager}; contains no simulation logic of its own.
 */
@EventBusSubscriber(modid = ThermalSystemsMod.MOD_ID)
public final class ThermalCommands {

    private ThermalCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("thermal")
                        .then(Commands.literal("zone")
                                .then(Commands.literal("create")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .then(Commands.argument("targetTemp", DoubleArgumentType.doubleArg())
                                                        .executes(context -> createZone(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "name"),
                                                                DoubleArgumentType.getDouble(context, "targetTemp"))))))
                                .then(Commands.literal("setBounds")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .then(Commands.argument("x1", IntegerArgumentType.integer())
                                                        .then(Commands.argument("y1", IntegerArgumentType.integer())
                                                                .then(Commands.argument("z1", IntegerArgumentType.integer())
                                                                        .then(Commands.argument("x2", IntegerArgumentType.integer())
                                                                                .then(Commands.argument("y2", IntegerArgumentType.integer())
                                                                                        .then(Commands.argument("z2", IntegerArgumentType.integer())
                                                                                                .executes(context -> setBounds(
                                                                                                        context.getSource(),
                                                                                                        StringArgumentType.getString(context, "name"),
                                                                                                        IntegerArgumentType.getInteger(context, "x1"),
                                                                                                        IntegerArgumentType.getInteger(context, "y1"),
                                                                                                        IntegerArgumentType.getInteger(context, "z1"),
                                                                                                        IntegerArgumentType.getInteger(context, "x2"),
                                                                                                        IntegerArgumentType.getInteger(context, "y2"),
                                                                                                        IntegerArgumentType.getInteger(context, "z2")))))))))))
                                .then(Commands.literal("clearBounds")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(context -> clearBounds(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name"))))))
                        .then(Commands.literal("source")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("zoneName", StringArgumentType.word())
                                                .then(Commands.argument("heatOutput", DoubleArgumentType.doubleArg())
                                                        .executes(context -> addSource(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "zoneName"),
                                                                DoubleArgumentType.getDouble(context, "heatOutput")))))))
                        .then(Commands.literal("radiator")
                                .then(Commands.literal("bind")
                                        .then(Commands.argument("zoneName", StringArgumentType.word())
                                                .executes(context -> bindRadiator(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "zoneName")))))
                                .then(Commands.literal("unbind")
                                        .executes(context -> unbindRadiator(context.getSource()))))
                        .then(Commands.literal("boiler")
                                .then(Commands.literal("setOutput")
                                        .then(Commands.argument("heatOutput", DoubleArgumentType.doubleArg())
                                                .executes(context -> setBoilerOutput(
                                                        context.getSource(),
                                                        DoubleArgumentType.getDouble(context, "heatOutput")))))));
    }

    private static int createZone(CommandSourceStack source, String name, double targetTemp) {
        ResourceKey<Level> level = source.getLevel().dimension();
        try {
            ClimateManager.get().createZone(level, name, targetTemp);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Created climate zone '" + name + "' with target " + targetTemp + "C."), true);
        return 1;
    }

    private static int setBounds(
            CommandSourceStack source, String zoneName, int x1, int y1, int z1, int x2, int y2, int z2) {
        ResourceKey<Level> level = source.getLevel().dimension();
        Optional<ClimateZone> zone = ClimateManager.get().getZoneByName(level, zoneName);
        if (zone.isEmpty()) {
            source.sendFailure(Component.literal("No climate zone named '" + zoneName + "' exists."));
            return 0;
        }

        zone.get().setBounds(new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2));
        source.sendSuccess(() -> Component.literal("Set bounds for zone '" + zoneName + "'."), true);
        return 1;
    }

    private static int clearBounds(CommandSourceStack source, String zoneName) {
        ResourceKey<Level> level = source.getLevel().dimension();
        Optional<ClimateZone> zone = ClimateManager.get().getZoneByName(level, zoneName);
        if (zone.isEmpty()) {
            source.sendFailure(Component.literal("No climate zone named '" + zoneName + "' exists."));
            return 0;
        }

        zone.get().clearBounds();
        source.sendSuccess(() -> Component.literal("Cleared bounds for zone '" + zoneName + "'."), true);
        return 1;
    }

    private static int addSource(CommandSourceStack source, String zoneName, double heatOutput) {
        ResourceKey<Level> level = source.getLevel().dimension();
        Optional<ClimateZone> zone = ClimateManager.get().getZoneByName(level, zoneName);
        if (zone.isEmpty()) {
            source.sendFailure(Component.literal("No climate zone named '" + zoneName + "' exists."));
            return 0;
        }

        HeatSource heatSource;
        try {
            heatSource = new HeatSource(heatOutput) {
            };
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }

        zone.get().getHeatSources().add(heatSource);
        source.sendSuccess(() -> Component.literal("Added heat source (" + heatOutput + "C/s) to zone '" + zoneName + "'."), true);
        return 1;
    }

    private static int bindRadiator(CommandSourceStack source, String zoneName) throws CommandSyntaxException {
        Optional<RadiatorBlockEntity> radiator = lookedAtRadiator(source);
        if (radiator.isEmpty()) {
            source.sendFailure(Component.literal("You must be looking at a Radiator."));
            return 0;
        }
        if (radiator.get().isBound()) {
            source.sendFailure(Component.literal("This radiator is already bound to a zone."));
            return 0;
        }

        ResourceKey<Level> level = source.getLevel().dimension();
        Optional<ClimateZone> zone = ClimateManager.get().getZoneByName(level, zoneName);
        if (zone.isEmpty()) {
            source.sendFailure(Component.literal("No climate zone named '" + zoneName + "' exists."));
            return 0;
        }

        radiator.get().bindTo(zone.get());
        source.sendSuccess(() -> Component.literal("Bound radiator to zone '" + zoneName + "'."), true);
        return 1;
    }

    private static int unbindRadiator(CommandSourceStack source) throws CommandSyntaxException {
        Optional<RadiatorBlockEntity> radiator = lookedAtRadiator(source);
        if (radiator.isEmpty()) {
            source.sendFailure(Component.literal("You must be looking at a Radiator."));
            return 0;
        }
        if (!radiator.get().isBound()) {
            source.sendFailure(Component.literal("This radiator is not currently bound to a zone."));
            return 0;
        }

        radiator.get().unbind();
        source.sendSuccess(() -> Component.literal("Unbound radiator from its zone."), true);
        return 1;
    }

    private static int setBoilerOutput(CommandSourceStack source, double heatOutput) throws CommandSyntaxException {
        Optional<BlockPos> targeted = lookedAtBlock(source.getPlayerOrException());
        BoilerBlockEntity boiler = targeted
                .map(pos -> source.getLevel().getBlockEntity(pos))
                .filter(BoilerBlockEntity.class::isInstance)
                .map(BoilerBlockEntity.class::cast)
                .orElse(null);
        if (boiler == null) {
            source.sendFailure(Component.literal("You must be looking at a Boiler."));
            return 0;
        }

        try {
            boiler.setHeatOutput(heatOutput);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Set boiler heat output to " + heatOutput + "C/s."), true);
        return 1;
    }

    private static Optional<RadiatorBlockEntity> lookedAtRadiator(CommandSourceStack source) throws CommandSyntaxException {
        Optional<BlockPos> targeted = lookedAtBlock(source.getPlayerOrException());
        return targeted
                .map(pos -> source.getLevel().getBlockEntity(pos))
                .filter(RadiatorBlockEntity.class::isInstance)
                .map(RadiatorBlockEntity.class::cast);
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
