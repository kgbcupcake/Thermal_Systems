package com.marie.thermalsystems.hover;

import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.heating.IHeatSource;
import dev.marie.framework.api.hover.BlockHoverProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Reports live heat/cooling output for whatever heat- or cooling-capable
 * block (Ender IO, Mekanism, PNC:R) the player is looking at.
 */
public final class ThermalHoverProvider implements BlockHoverProvider {

    @Override
    public boolean supports(Level level, BlockPos pos, BlockState state) {
        return HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, pos, null, null, null) != null
                || CoolingSourceCapabilities.COOLING_SOURCE.getCapability(level, pos, null, null, null) != null;
    }

    @Override
    public CompoundTag computeServerData(ServerLevel level, BlockPos pos, ServerPlayer player) {
        IHeatSource heatSource = HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, pos, null, null, null);
        ICoolingSource coolingSource = CoolingSourceCapabilities.COOLING_SOURCE.getCapability(level, pos, null, null, null);

        CompoundTag data = new CompoundTag();
        data.putDouble("heat", heatSource != null ? heatSource.getHeatOutput() : 0.0);
        data.putDouble("cooling", coolingSource != null ? coolingSource.getCoolingOutput() : 0.0);
        return data;
    }

    @Override
    public List<Component> renderLines(CompoundTag data, Level level, BlockPos pos) {
        double heat = data.getDouble("heat");
        double cooling = data.getDouble("cooling");

        if (heat == 0.0 && cooling == 0.0) {
            return List.of(Component.literal("No thermal output"));
        }

        List<Component> lines = new java.util.ArrayList<>();
        if (heat != 0.0) {
            lines.add(Component.literal(String.format("Heat Output: %.2fC/s", heat)));
        }
        if (cooling != 0.0) {
            lines.add(Component.literal(String.format("Cooling Output: %.2fC/s", cooling)));
        }
        return lines;
    }
}
