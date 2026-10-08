package com.marie.thermalsystems.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Celsius added on top of {@code defaultAmbientTemperature} by whichever season mod is installed.
 * Ecliptic Seasons is asked first when both are present, since its solar-term climate already
 * covers the same year Serene Seasons would. A provider that doesn't apply (mod switched off, or
 * the dimension has no seasons) returns empty so the next one can answer.
 */
public final class SeasonOffset {

    /**
     * Minecraft stores biome climate in its own units (plains is 0.8, snow starts near 0.15).
     * One of those units is about 30°C, so a winter swing of -0.45 lands near -13°C.
     */
    public static final double CELSIUS_PER_BIOME_UNIT = 30.0;

    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonOffset.class);
    private static final List<Provider> PROVIDERS = new ArrayList<>();

    private SeasonOffset() {
    }

    public static void register(Provider provider) {
        PROVIDERS.removeIf(existing -> existing.name().equals(provider.name()));
        PROVIDERS.add(provider);
    }

    /** Seasonal shift at {@code pos}, or 0 when no season mod applies. */
    public static double celsius(Level level, BlockPos pos) {
        for (Provider provider : PROVIDERS) {
            try {
                OptionalDouble offset = provider.offset(level, pos);
                if (offset.isPresent()) {
                    return offset.getAsDouble();
                }
            } catch (Throwable thrown) {
                if (provider.noteFailure()) {
                    LOGGER.warn("[MTS] Season temperature from {} failed; ignoring it until restart",
                            provider.name(), thrown);
                }
            }
        }
        return 0.0;
    }

    /** {@code adjusted - plain} biome temperature, in Celsius. Non-finite input contributes nothing. */
    public static double fromBiomeDelta(double adjustedBiomeTemperature, double plainBiomeTemperature) {
        double delta = adjustedBiomeTemperature - plainBiomeTemperature;
        if (!Double.isFinite(delta)) {
            return 0.0;
        }
        return delta * CELSIUS_PER_BIOME_UNIT;
    }

    public interface Provider {
        String name();

        OptionalDouble offset(Level level, BlockPos pos) throws Exception;

        /** @return true the first time this provider fails, so the warning is logged once */
        boolean noteFailure();
    }
}
