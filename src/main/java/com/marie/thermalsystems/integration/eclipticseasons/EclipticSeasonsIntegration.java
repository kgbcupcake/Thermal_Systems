package com.marie.thermalsystems.integration.eclipticseasons;

import com.marie.thermalsystems.climate.SeasonOffset;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optional integration with Ecliptic Seasons. Only ever initialized when that mod is present.
 * Talks to Ecliptic through reflection so this project stays on Java 21: recent Ecliptic builds
 * are compiled for a newer Java and cannot sit on the compile classpath.
 *
 * <p>{@code EclipticUtil.getTemperatureFloat} is the vanilla biome temperature plus the current
 * solar term's change. The difference from the biome's base temperature, scaled to Celsius, is
 * what winter and summer add on top of Default Ambient Temperature.
 */
public final class EclipticSeasonsIntegration {

    public static final String MOD_ID = "eclipticseasons";

    private static final Logger LOGGER = LoggerFactory.getLogger(EclipticSeasonsIntegration.class);
    private static final String API = "com.teamtea.eclipticseasons.api.EclipticSeasonsApi";
    private static final String UTIL = "com.teamtea.eclipticseasons.api.util.EclipticUtil";

    private EclipticSeasonsIntegration() {
    }

    public static void init() {
        try {
            Class<?> apiClass = Class.forName(API);
            Class<?> utilClass = Class.forName(UTIL);
            Method getInstance = apiClass.getMethod("getInstance");
            Method isSeasonEnabled = apiClass.getMethod("isSeasonEnabled", Level.class);
            Method getTemperatureFloat = utilClass.getMethod("getTemperatureFloat", Level.class, Biome.class, BlockPos.class);
            SeasonOffset.register(new Provider(getInstance, isSeasonEnabled, getTemperatureFloat));
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("[MTS] Ecliptic Seasons is installed, but its temperature API could not be hooked", exception);
        }
    }

    private static final class Provider implements SeasonOffset.Provider {
        private final Method getInstance;
        private final Method isSeasonEnabled;
        private final Method getTemperatureFloat;
        private final AtomicBoolean failed = new AtomicBoolean();

        private Provider(Method getInstance, Method isSeasonEnabled, Method getTemperatureFloat) {
            this.getInstance = getInstance;
            this.isSeasonEnabled = isSeasonEnabled;
            this.getTemperatureFloat = getTemperatureFloat;
        }

        @Override
        public String name() {
            return MOD_ID;
        }

        @Override
        public OptionalDouble offset(Level level, BlockPos pos) throws ReflectiveOperationException {
            if (!ThermalConfig.ECLIPTIC_SEASONS_ENABLED.get()) {
                return OptionalDouble.empty();
            }
            Object api = getInstance.invoke(null);
            if (api == null || !(boolean) isSeasonEnabled.invoke(api, level)) {
                return OptionalDouble.empty();
            }
            Biome biome = level.getBiome(pos).value();
            float adjusted = (float) getTemperatureFloat.invoke(null, level, biome, pos);
            return OptionalDouble.of(SeasonOffset.fromBiomeDelta(adjusted, biome.getBaseTemperature()));
        }

        @Override
        public boolean noteFailure() {
            return failed.compareAndSet(false, true);
        }
    }
}
