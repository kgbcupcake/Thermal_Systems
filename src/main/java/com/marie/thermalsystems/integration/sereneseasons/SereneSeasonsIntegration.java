package com.marie.thermalsystems.integration.sereneseasons;

import com.marie.thermalsystems.climate.SeasonOffset;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import sereneseasons.season.SeasonHooks;

import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optional integration with Serene Seasons. Only ever initialized when that mod is present.
 * Serene Seasons imports exist only within this package.
 *
 * <p>{@link SeasonHooks#getBiomeTemperature} already applies that mod's per-sub-season
 * adjustment (and skips dimensions and biomes it doesn't touch). The difference from the
 * plain biome temperature is the Celsius shift added on top of Default Ambient Temperature.
 * Registered after Ecliptic Seasons, so it only answers when Ecliptic isn't applying.
 */
public final class SereneSeasonsIntegration {

    public static final String MOD_ID = "sereneseasons";

    private SereneSeasonsIntegration() {
    }

    public static void init() {
        SeasonOffset.register(new SeasonOffset.Provider() {
            private final AtomicBoolean failed = new AtomicBoolean();

            @Override
            public String name() {
                return MOD_ID;
            }

            @Override
            public OptionalDouble offset(Level level, BlockPos pos) {
                if (!ThermalConfig.SERENE_SEASONS_ENABLED.get()) {
                    return OptionalDouble.empty();
                }
                Holder<Biome> biome = level.getBiome(pos);
                float adjusted = SeasonHooks.getBiomeTemperature(level, biome, pos);
                return OptionalDouble.of(SeasonOffset.fromBiomeDelta(adjusted, biome.value().getBaseTemperature()));
            }

            @Override
            public boolean noteFailure() {
                return failed.compareAndSet(false, true);
            }
        });
    }
}
