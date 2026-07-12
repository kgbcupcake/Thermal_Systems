package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.zone.ZoneSnapshot;
import com.marie.thermalsystems.integration.enderio.block.EnderIOThermalAdapterBlock;
import com.marie.thermalsystems.integration.enderio.blockentity.EnderIOThermalAdapterBlockEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/**
 * Optional integration with Ender IO. Only ever loaded and initialized when
 * Ender IO is present - see the {@code ModList.isLoaded} guard around
 * {@link #init(IEventBus)} in {@link ThermalSystemsMod}.
 *
 * <p>Verified against the Ender IO 1.21.1 source (Team-EnderIO/EnderIO):
 * Ender IO defines no heat capability and has no native heat concept. Its
 * "heat conduit" only exists in the separate, Mekanism-dependent
 * {@code enderio-modded-conduits} module and relays Mekanism's own
 * {@code IHeatHandler} capability - not anything Ender IO owns. Ender IO's
 * native Energy Conduit ({@code EnergyConduit.canConnectToBlock}) connects to
 * any neighboring block exposing the standard NeoForge
 * {@code Capabilities.EnergyStorage.BLOCK} capability, exactly like it
 * connects to any FE-consuming machine. That capability, registered below,
 * is the real and only Ender IO integration surface this mod uses - it
 * requires no Ender IO classes or Ender IO jar dependency at all, so no
 * Ender IO imports exist anywhere in this package.
 */
public final class EnderIOIntegration {

    public static final String ENDERIO_MOD_ID = "enderio";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThermalSystemsMod.MOD_ID);
    public static final DeferredBlock<EnderIOThermalAdapterBlock> THERMAL_ADAPTER = BLOCKS.register("enderio_thermal_adapter",
            () -> new EnderIOThermalAdapterBlock(BlockBehaviour.Properties.of().strength(3.0f)));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThermalSystemsMod.MOD_ID);
    public static final DeferredItem<BlockItem> THERMAL_ADAPTER_ITEM =
            ITEMS.registerSimpleBlockItem("enderio_thermal_adapter", THERMAL_ADAPTER);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThermalSystemsMod.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderIOThermalAdapterBlockEntity>> THERMAL_ADAPTER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("enderio_thermal_adapter",
                    () -> BlockEntityType.Builder.of(EnderIOThermalAdapterBlockEntity::new, THERMAL_ADAPTER.get()).build(null));

    private EnderIOIntegration() {
    }

    public static void init(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(RegisterCapabilitiesEvent.class, EnderIOIntegration::onRegisterCapabilities);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, EnderIOIntegration::onRegisterCommands);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(HeatSourceCapabilities.HEAT_SOURCE, THERMAL_ADAPTER_BLOCK_ENTITY.get(),
                (adapter, context) -> adapter);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, THERMAL_ADAPTER_BLOCK_ENTITY.get(),
                (adapter, side) -> adapter);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("thermal")
                        .then(Commands.literal("enderio")
                                .then(Commands.literal("bind")
                                        .then(Commands.argument("zoneName", StringArgumentType.word())
                                                .executes(context -> bindAdapter(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "zoneName")))))
                                .then(Commands.literal("unbind")
                                        .executes(context -> unbindAdapter(context.getSource())))));
    }

    private static int bindAdapter(CommandSourceStack source, String zoneName) throws CommandSyntaxException {
        Optional<EnderIOThermalAdapterBlockEntity> adapter = lookedAtAdapter(source);
        if (adapter.isEmpty()) {
            source.sendFailure(Component.literal("You must be looking at an Ender IO Thermal Adapter."));
            return 0;
        }

        Level level = source.getLevel();
        Optional<ZoneSnapshot> zone = ThermalSystemsAPI.getZoneByName(level, zoneName);
        if (zone.isEmpty()) {
            source.sendFailure(Component.literal("No climate zone named '" + zoneName + "' exists."));
            return 0;
        }

        BlockPos pos = adapter.get().getBlockPos();
        try {
            ThermalSystemsAPI.bindHeatSource(level, pos, zone.get().id());
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Bound Ender IO Thermal Adapter to zone '" + zoneName + "'."), true);
        return 1;
    }

    private static int unbindAdapter(CommandSourceStack source) throws CommandSyntaxException {
        Optional<EnderIOThermalAdapterBlockEntity> adapter = lookedAtAdapter(source);
        if (adapter.isEmpty()) {
            source.sendFailure(Component.literal("You must be looking at an Ender IO Thermal Adapter."));
            return 0;
        }

        Level level = source.getLevel();
        BlockPos pos = adapter.get().getBlockPos();
        ThermalSystemsAPI.unbindHeatSource(level, pos);

        source.sendSuccess(() -> Component.literal("Unbound Ender IO Thermal Adapter from its zone."), true);
        return 1;
    }

    private static Optional<EnderIOThermalAdapterBlockEntity> lookedAtAdapter(CommandSourceStack source) throws CommandSyntaxException {
        Optional<BlockPos> targeted = lookedAtBlock(source.getPlayerOrException());
        return targeted
                .map(pos -> source.getLevel().getBlockEntity(pos))
                .filter(EnderIOThermalAdapterBlockEntity.class::isInstance)
                .map(EnderIOThermalAdapterBlockEntity.class::cast);
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
