package com.marie.thermalsystems.integration.mekanism;

import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.data.config.ThermalConfig;
import dev.marie.framework.api.hover.BlockHoverProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Jade tooltip for a point on a Mekanism thermodynamic conductor network:
 * one line per connected Thermal-Systems-capable machine reachable on that
 * network, plus a final network-total line. Distinct from the generic
 * {@link com.marie.thermalsystems.hover.ThermalHoverProvider}, which already
 * covers individual source machines directly and would otherwise just show
 * this network position's already-summed {@code getHeatOutput()} with no
 * per-source breakdown.
 */
public final class MekanismNetworkHoverProvider implements BlockHoverProvider {

    @Override
    public boolean supports(Level level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && MekanismIntegration.TRANSMITTER_BLOCK_ENTITY_TYPES.contains(blockEntity.getType());
    }

    @Override
    public CompoundTag computeServerData(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Set<BlockPos> boundary = MekanismNetworkDiscovery.discoverBoundary(pos, level, MekanismIntegration.TRANSMITTER_BLOCK_ENTITY_TYPES);

        ListTag connected = new ListTag();
        double totalHeat = 0.0;
        double totalCooling = 0.0;
        for (BlockPos boundaryPos : boundary) {
            IHeatSource heatSource = HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, boundaryPos, null, null, null);
            ICoolingSource coolingSource = CoolingSourceCapabilities.COOLING_SOURCE.getCapability(level, boundaryPos, null, null, null);
            if (heatSource == null && coolingSource == null) {
                continue;
            }

            double heat = heatSource != null ? heatSource.getHeatOutput() : 0.0;
            double cooling = coolingSource != null ? coolingSource.getCoolingOutput() : 0.0;

            CompoundTag entry = new CompoundTag();
            entry.putString("name", displayName(level, boundaryPos));
            entry.putDouble("heat", heat);
            entry.putDouble("cooling", cooling);
            connected.add(entry);

            totalHeat += heat;
            totalCooling += cooling;
        }

        CompoundTag data = new CompoundTag();
        data.put("connected", connected);
        data.putDouble("totalHeat", totalHeat);
        data.putDouble("totalCooling", totalCooling);
        return data;
    }

    @Override
    public List<Component> renderLines(CompoundTag data, Level level, BlockPos pos) {
        if (!ThermalConfig.HOVER_TOOLTIPS_ENABLED.get()) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>();

        ListTag connected = data.getList("connected", 10);
        if (connected.isEmpty()) {
            lines.add(Component.literal("No connected heat sources"));
        } else {
            for (int i = 0; i < connected.size(); i++) {
                CompoundTag entry = connected.getCompound(i);
                String name = entry.getString("name");
                double heat = entry.getDouble("heat");
                double cooling = entry.getDouble("cooling");
                lines.add(Component.literal(String.format(Locale.ROOT, "%s: %s", name, statsText(heat, cooling))));
            }
        }

        lines.add(Component.literal(String.format(Locale.ROOT,
                "Network Total: %s", statsText(data.getDouble("totalHeat"), data.getDouble("totalCooling")))));
        return lines;
    }

    private static String statsText(double heat, double cooling) {
        if (heat != 0.0 && cooling != 0.0) {
            return String.format(Locale.ROOT, "%.2fC/s heat, %.2fC/s cooling", heat, cooling);
        }
        if (cooling != 0.0) {
            return String.format(Locale.ROOT, "%.2fC/s cooling", cooling);
        }
        return String.format(Locale.ROOT, "%.2fC/s heat", heat);
    }

    private static String displayName(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return "Unknown";
        }
        String path = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).getPath();
        String[] words = path.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }
}
