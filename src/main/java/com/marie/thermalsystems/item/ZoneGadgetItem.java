package com.marie.thermalsystems.item;

import com.marie.thermalsystems.climate.ClimateManager;
import com.marie.thermalsystems.controller.ClimateController;
import com.marie.thermalsystems.controller.PlayerTemperatureUnits;
import com.marie.thermalsystems.controller.TemperatureUnit;
import com.marie.thermalsystems.controller.ThermalScreens;
import com.marie.thermalsystems.network.ZoneInfo;
import com.marie.thermalsystems.network.ZonePayloads;
import com.marie.thermalsystems.registry.ThermalRegistries;
import com.marie.thermalsystems.zone.ClimateZone;
import com.marie.thermalsystems.zone.ZoneSpatialIndex;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Marks out climate zones by clicking blocks, in the spirit of Building Gadgets' Copy-Paste Gadget:
 * right-click two blocks to select the corners of a box (previewed in-world as a wireframe), and the
 * zone editor opens to fine-tune the box in 3D, name it and save. Sneak + right-click opens the
 * gadget's menu (modes, and reopening the editor). The current {@link GadgetMode} decides what a
 * click does; see each mode's javadoc.
 *
 * <p>All selection state lives in the stack's data components and is only ever changed on the
 * server. The box the editor saves is re-validated by {@link ClimateController} like any edit.
 */
public class ZoneGadgetItem extends Item {

    public ZoneGadgetItem(Properties properties) {
        super(properties);
    }

    public static GadgetMode mode(ItemStack stack) {
        return stack.getOrDefault(ThermalRegistries.GADGET_MODE.get(), GadgetMode.CREATE);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.thermalsystems.zone_gadget.with_mode",
                super.getName(stack), Component.translatable(mode(stack).translationKey()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            ThermalScreens.openGadget();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (player.isSecondaryUseActive()) {
            if (level.isClientSide) {
                ThermalScreens.openGadget();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        switch (mode(stack)) {
            case CREATE -> selectCorner(stack, pos, serverPlayer);
            case EDIT -> editClick(stack, pos, serverPlayer);
            case INSPECT -> inspect(pos, serverPlayer);
            case COPY -> copyOrPaste(stack, pos, serverPlayer);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(mode(stack).translationKey() + ".hint").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.thermalsystems.zone_gadget.menu_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** First click (or any click once both are set) sets corner one; the next sets corner two. */
    private static void selectCorner(ItemStack stack, BlockPos pos, ServerPlayer player) {
        BlockPos first = stack.get(ThermalRegistries.CORNER_ONE.get());
        BlockPos second = stack.get(ThermalRegistries.CORNER_TWO.get());
        if (first == null || second != null) {
            stack.set(ThermalRegistries.CORNER_ONE.get(), pos.immutable());
            stack.remove(ThermalRegistries.CORNER_TWO.get());
            player.displayClientMessage(Component.translatable("message.thermalsystems.gadget.corner_one",
                    pos.getX(), pos.getY(), pos.getZ()), true);
            return;
        }
        stack.set(ThermalRegistries.CORNER_TWO.get(), pos.immutable());
        player.displayClientMessage(Component.translatable("message.thermalsystems.gadget.corner_two",
                Math.abs(first.getX() - pos.getX()) + 1,
                Math.abs(first.getY() - pos.getY()) + 1,
                Math.abs(first.getZ() - pos.getZ()) + 1,
                ClimateController.volume(first, pos)), true);
        Optional<ClimateZone> editing = editTarget(stack, player);
        openEditor(player, first, pos.immutable(), editing.orElse(null));
    }

    private static void openEditor(ServerPlayer player, BlockPos first, BlockPos second, @Nullable ClimateZone editing) {
        PacketDistributor.sendToPlayer(player, new ZonePayloads.OpenEditor(first, second,
                Optional.ofNullable(editing).map(ClimateZone::getId), editing == null ? "" : editing.getName()));
    }

    private static void editClick(ItemStack stack, BlockPos pos, ServerPlayer player) {
        if (editTarget(stack, player).isPresent()) {
            selectCorner(stack, pos, player);
            return;
        }
        Optional<ClimateZone> zone = ZoneSpatialIndex.resolve(player.level(), pos);
        if (zone.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.thermalsystems.gadget.no_zone_here"), true);
            return;
        }
        if (!ClimateController.canEdit(player, zone.get())) {
            player.displayClientMessage(Component.translatable("message.thermalsystems.zone.not_owner", zone.get().getName()), true);
            return;
        }
        stack.set(ThermalRegistries.EDIT_TARGET.get(), zone.get().getId());
        stack.set(ThermalRegistries.CORNER_ONE.get(), zone.get().getBoundsMin());
        stack.set(ThermalRegistries.CORNER_TWO.get(), zone.get().getBoundsMax());
        player.displayClientMessage(Component.translatable("message.thermalsystems.gadget.editing", zone.get().getName()), true);
        openEditor(player, zone.get().getBoundsMin(), zone.get().getBoundsMax(), zone.get());
    }

    private static void inspect(BlockPos pos, ServerPlayer player) {
        Optional<ClimateZone> zone = ZoneSpatialIndex.resolve(player.level(), pos);
        if (zone.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.thermalsystems.gadget.no_zone_here"), true);
            return;
        }
        ZoneInfo info = ZoneInfo.of(player, zone.get());
        TemperatureUnit unit = PlayerTemperatureUnits.of(player);
        String owner = info.ownerName().isEmpty()
                ? Component.translatable("gui.thermalsystems.thermostat.owner_ops").getString()
                : info.ownerName();
        player.sendSystemMessage(Component.translatable("message.thermalsystems.gadget.inspect.header", info.name(), owner)
                .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("message.thermalsystems.gadget.inspect.climate",
                unit.format(info.currentTemp()), unit.format(info.targetTemp()),
                Component.translatable("gui.thermalsystems.thermostat.mode." + info.mode().name().toLowerCase(Locale.ROOT)),
                Component.translatable("gui.thermalsystems.thermostat.demand." + info.demand().name().toLowerCase(Locale.ROOT))));
        player.sendSystemMessage(Component.translatable("message.thermalsystems.gadget.inspect.sources",
                info.heatSources(), String.format(Locale.ROOT, "%.1f", info.heatOutput()),
                info.coolingSources(), String.format(Locale.ROOT, "%.1f", info.coolingOutput())));
        BlockPos min = info.min();
        BlockPos max = info.max();
        player.sendSystemMessage(Component.translatable("message.thermalsystems.gadget.inspect.size",
                max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1, info.volume()));
    }

    private static void copyOrPaste(ItemStack stack, BlockPos pos, ServerPlayer player) {
        Optional<ClimateZone> zone = ZoneSpatialIndex.resolve(player.level(), pos);
        if (zone.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.thermalsystems.gadget.no_zone_here"), true);
            return;
        }
        CopiedSettings copied = stack.get(ThermalRegistries.COPIED_SETTINGS.get());
        if (copied == null) {
            ClimateZone source = zone.get();
            stack.set(ThermalRegistries.COPIED_SETTINGS.get(),
                    new CopiedSettings(source.getName(), source.getTargetTemp(), source.getMode()));
            player.displayClientMessage(Component.translatable("message.thermalsystems.gadget.copied",
                    source.getName(), PlayerTemperatureUnits.of(player).format(source.getTargetTemp()),
                    source.getMode().name()), true);
            return;
        }
        ClimateController.Result result = ClimateController.updateSettings(
                player, zone.get().getId(), copied.target(), copied.mode(), zone.get().isPublicControl());
        player.displayClientMessage(result.success()
                ? Component.translatable("message.thermalsystems.gadget.pasted", copied.sourceName(), zone.get().getName())
                : result.message(), true);
    }

    /** The zone the gadget is editing, clearing a stale reference to a zone that no longer exists. */
    private static Optional<ClimateZone> editTarget(ItemStack stack, ServerPlayer player) {
        UUID id = stack.get(ThermalRegistries.EDIT_TARGET.get());
        if (id == null) {
            return Optional.empty();
        }
        Optional<ClimateZone> zone = ClimateManager.get().getZone(player.serverLevel().dimension(), id);
        if (zone.isEmpty()) {
            stack.remove(ThermalRegistries.EDIT_TARGET.get());
        }
        return zone;
    }

    @Nullable
    public static ItemStack heldGadget(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof ZoneGadgetItem) {
                return stack;
            }
        }
        return null;
    }

    private static void clearSelection(ItemStack stack) {
        stack.remove(ThermalRegistries.CORNER_ONE.get());
        stack.remove(ThermalRegistries.CORNER_TWO.get());
        stack.remove(ThermalRegistries.EDIT_TARGET.get());
    }

    public static void applyUpdate(ServerPlayer player, ZonePayloads.GadgetUpdate update) {
        ItemStack stack = heldGadget(player);
        if (stack == null) {
            return;
        }
        if (mode(stack) != update.mode()) {
            clearSelection(stack);
            stack.set(ThermalRegistries.GADGET_MODE.get(), update.mode());
        }
        if (update.clearSelection()) {
            stack.remove(ThermalRegistries.CORNER_ONE.get());
            stack.remove(ThermalRegistries.CORNER_TWO.get());
        }
        if (update.stopEditing()) {
            clearSelection(stack);
        }
        if (update.clearCopy()) {
            stack.remove(ThermalRegistries.COPIED_SETTINGS.get());
        }
    }

    /**
     * The zone editor's Save. The box is kept on the gadget first, so if the server turns it down
     * the in-world preview and a reopened editor still show what the player was working on.
     */
    public static ClimateController.Result saveFromEditor(ServerPlayer player, ZonePayloads.GadgetSave save) {
        ItemStack stack = heldGadget(player);
        if (stack == null) {
            return ClimateController.Result.fail("no_gadget");
        }
        stack.set(ThermalRegistries.CORNER_ONE.get(), save.first().immutable());
        stack.set(ThermalRegistries.CORNER_TWO.get(), save.second().immutable());
        ClimateController.Result result = save.editing()
                .map(id -> ClimateController.edit(player, id, save.name(), save.first(), save.second()))
                .orElseGet(() -> ClimateController.create(player, save.name(), save.first(), save.second(), save.target()));
        if (result.success()) {
            clearSelection(stack);
        }
        return result;
    }
}
