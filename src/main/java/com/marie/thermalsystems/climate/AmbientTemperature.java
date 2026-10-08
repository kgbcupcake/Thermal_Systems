package com.marie.thermalsystems.climate;

import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.zone.ClimateZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The air temperature Thermal Systems drifts toward: the configured default, plus the season
 * shift at a position when a season mod is installed.
 */
public final class AmbientTemperature {

    private AmbientTemperature() {
    }

    public static double configured() {
        return ThermalConfig.DEFAULT_AMBIENT_TEMPERATURE.get();
    }

    public static double at(Level level, BlockPos pos) {
        return configured() + SeasonOffset.celsius(level, pos);
    }

    /** Center of the zone when it has bounds; otherwise the configured default, with no season shift. */
    public static double forZone(@Nullable ServerLevel level, ClimateZone zone) {
        if (level == null || !zone.hasBounds()) {
            return configured();
        }
        BlockPos min = zone.getBoundsMin();
        BlockPos max = zone.getBoundsMax();
        BlockPos sample = new BlockPos(
                (min.getX() + max.getX()) / 2,
                (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2);
        return at(level, sample);
    }
}
