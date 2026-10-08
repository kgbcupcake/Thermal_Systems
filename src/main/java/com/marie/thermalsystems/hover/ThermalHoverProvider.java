package com.marie.thermalsystems.hover;

import com.marie.thermalsystems.api.climate.IHeatPump;
import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.controller.ClimateMode;
import com.marie.thermalsystems.controller.TemperatureUnit;
import com.marie.thermalsystems.data.config.ThermalClientConfig;
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
        double heat = heatSource != null ? heatSource.getHeatOutput() : 0.0;
        double cooling = coolingSource != null ? coolingSource.getCoolingOutput() : 0.0;
        data.putDouble("heat", heat);
        data.putDouble("cooling", cooling);
        IHeatPump pump = heatSource instanceof IHeatPump p ? p : coolingSource instanceof IHeatPump p ? p : null;
        if (pump != null) {
            data.putDouble("pump", pump.getPumpOutput());
            pump.getControllingZone().ifPresent(zone -> {
                data.putString("zone", zone.getName());
                data.putString("zoneState", zone.getMode() == ClimateMode.OFF ? "zone switched off" : "zone not calling");
                data.putDouble("zoneTemp", zone.getCurrentTemp());
                data.putDouble("zoneTarget", zone.getTargetTemp());
            });
        }
        return data;
    }

    @Override
    public List<Component> renderLines(CompoundTag data, Level level, BlockPos pos) {
        double heat = data.getDouble("heat");
        double cooling = data.getDouble("cooling");

        if (data.contains("pump")) {
            return pumpLines(data, heat, cooling);
        }
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

    private static List<Component> pumpLines(CompoundTag data, double heat, double cooling) {
        List<Component> lines = new java.util.ArrayList<>();
        String zone = data.getString("zone");
        TemperatureUnit unit = ThermalClientConfig.unit();
        String room = zone.isEmpty() ? "" : " - room " + unit.format(data.getDouble("zoneTemp"))
                + ", target " + unit.format(data.getDouble("zoneTarget"));
        if (heat > 0.0) {
            lines.add(Component.literal("Heating" + room));
        } else if (cooling > 0.0) {
            lines.add(Component.literal("Cooling" + room));
        } else if (!zone.isEmpty()) {
            lines.add(Component.literal("Standby (" + data.getString("zoneState") + ")" + room));
        } else {
            lines.add(Component.literal("No thermal output"));
        }
        lines.add(Component.literal(zone.isEmpty()
                ? "Not in a zone - uses its default mode"
                : "Controlled by zone: " + zone));
        return lines;
    }
}
