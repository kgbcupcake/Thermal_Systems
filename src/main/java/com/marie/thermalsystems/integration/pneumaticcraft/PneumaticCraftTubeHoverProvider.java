package com.marie.thermalsystems.integration.pneumaticcraft;

import com.marie.thermalsystems.data.config.ThermalConfig;
import dev.marie.framework.api.hover.BlockHoverProvider;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Jade tooltip for PneumaticCraft's Pressure Tube network blocks. PNC:R's
 * heat model ({@code IHeatExchangerLogic}) is purely local block-to-block
 * conduction with no discrete network to enumerate - confirmed by
 * disassembling the PNC:R 8.2.20+mc1.21.1 jar: {@code IHeatExchangerLogic}
 * only exposes peer-to-peer {@code addConnectedExchanger}, and
 * {@code PressureTubeBlockEntity} implements only air-pressure interfaces
 * ({@code IAirListener}/{@code AbstractAirHandlingBlockEntity}), carrying no
 * heat data at all. So unlike the Mekanism/Ender IO network hover providers,
 * this shows connectivity/pressure info only, via PNC:R's own
 * {@link PNCCapabilities#AIR_HANDLER_MACHINE} capability - never a heat-
 * network sum, since there is nothing to sum. Purely informational: no
 * binding, no simulation effect, no {@code ThermalSystemsAPI} interaction.
 */
public final class PneumaticCraftTubeHoverProvider implements BlockHoverProvider {

    private static final Set<String> TUBE_BLOCK_ENTITY_PATHS = Set.of(
            "pressure_tube",
            "advanced_pressure_tube",
            "reinforced_pressure_tube",
            "tube_junction");

    @Override
    public boolean supports(Level level, BlockPos pos, BlockState state) {
        return isTube(level, pos);
    }

    @Override
    public CompoundTag computeServerData(ServerLevel level, BlockPos pos, ServerPlayer player) {
        IAirHandlerMachine airHandler = PNCCapabilities.AIR_HANDLER_MACHINE.getCapability(level, pos, null, null, null);

        CompoundTag data = new CompoundTag();
        if (airHandler != null) {
            data.putBoolean("hasAir", true);
            data.putFloat("pressure", airHandler.getPressure());
            data.putInt("air", airHandler.getAir());
            data.putInt("volume", airHandler.getVolume());
        } else {
            data.putBoolean("hasAir", false);
        }
        return data;
    }

    @Override
    public List<Component> renderLines(CompoundTag data, Level level, BlockPos pos) {
        if (!ThermalConfig.HOVER_TOOLTIPS_ENABLED.get()) {
            return List.of();
        }
        if (!data.getBoolean("hasAir")) {
            return List.of(Component.literal("Not connected to a pressure network"));
        }

        return List.of(
                Component.literal(String.format(Locale.ROOT, "Pressure: %.2f bar", data.getFloat("pressure"))),
                Component.literal(String.format(Locale.ROOT, "Air: %d / %d", data.getInt("air"), data.getInt("volume"))));
    }

    private static boolean isTube(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }
        BlockEntityType<?> type = blockEntity.getType();
        for (String path : TUBE_BLOCK_ENTITY_PATHS) {
            BlockEntityType<?> tubeType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(
                    ResourceLocation.fromNamespaceAndPath(PneumaticCraftIntegration.PNC_MOD_ID, path));
            if (type == tubeType) {
                return true;
            }
        }
        return false;
    }
}
