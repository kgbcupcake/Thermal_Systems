package com.marie.thermalsystems.item;

import com.marie.thermalsystems.controller.ThermalScreens;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Handheld thermostat: right-click to open the thermostat app (every nearby zone you can see, and
 * each one's thermostat). Sneak + right-click the side of a block to mount it there as a wall
 * thermostat for the room it's in - it's the wall thermostat's block item, so breaking the wall
 * thermostat gives the tablet back.
 */
public class ThermostatTabletItem extends BlockItem {

    public ThermostatTabletItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public String getDescriptionId() {
        return getOrCreateDescriptionId();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            return super.useOn(context);
        }
        if (context.getLevel().isClientSide) {
            ThermalScreens.openThermostat(null, null);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            ThermalScreens.openThermostat(null, null);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thermalsystems.thermostat_tablet.hint.open").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.thermalsystems.thermostat_tablet.hint.mount").withStyle(ChatFormatting.GRAY));
    }
}
