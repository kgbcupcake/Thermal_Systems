package com.marie.thermalsystems.integration.enderio;

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
 * Jade tooltip for a point on an Ender IO conduit network: one line per
 * connected Thermal-Systems-capable machine reachable on that network, plus
 * a final network-total line. Distinct from the generic
 * {@link com.marie.thermalsystems.hover.ThermalHoverProvider}, which already
 * covers the Stirling Generator itself directly and would otherwise just
 * show this network position's already-summed {@code getHeatOutput()} with
 * no per-source breakdown.
 */
public final class EnderIONetworkHoverProvider implements BlockHoverProvider {

    @Override
    public boolean supports(Level level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && blockEntity.getType() == EnderIOIntegration.CONDUIT_BLOCK_ENTITY_TYPE;
    }

    @Override
    public CompoundTag computeServerData(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Set<BlockPos> boundary = EnderIONetworkDiscovery.discoverBoundary(pos, level, EnderIOIntegration.CONDUIT_BLOCK_ENTITY_TYPE);

        ListTag connected = new ListTag();
        double totalHeat = 0.0;
        for (BlockPos boundaryPos : boundary) {
            IHeatSource heatSource = HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, boundaryPos, null, null, null);
            if (heatSource == null) {
                continue;
            }

            double heat = heatSource.getHeatOutput();

            CompoundTag entry = new CompoundTag();
            entry.putString("name", displayName(level, boundaryPos));
            entry.putDouble("heat", heat);
            connected.add(entry);

            totalHeat += heat;
        }

        CompoundTag data = new CompoundTag();
        data.put("connected", connected);
        data.putDouble("totalHeat", totalHeat);
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
                lines.add(Component.literal(String.format(Locale.ROOT,
                        "%s: %.2fC/s heat", entry.getString("name"), entry.getDouble("heat"))));
            }
        }

        lines.add(Component.literal(String.format(Locale.ROOT, "Network Total: %.2fC/s heat", data.getDouble("totalHeat"))));
        return lines;
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
