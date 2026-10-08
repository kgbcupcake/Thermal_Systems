package com.marie.thermalsystems.integration.lso;

import com.marie.thermalsystems.api.bridge.ITemperatureBridge;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureEnum;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges Thermal Systems' resolved per-player ambient temperature into
 * Legendary Survival Overhaul via its public {@link TemperatureUtil} API.
 * Registered from {@link LSOIntegration#init()}, itself only ever called
 * when LSO is present.
 *
 * <p>Scale check: {@code TemperatureUtil.addTemperatureModifier} feeds
 * LSO's {@code HEATING_TEMPERATURE}/{@code COOLING_TEMPERATURE} attributes,
 * which are additive deltas summed on top of a base temperature LSO computes
 * itself from the world/biome/weather (see LSO's internal
 * {@code AttributeModifier#getPlayerInfluence} and
 * {@code TemperatureUtilInternal#getPlayerTargetTemperature}) - it is not an
 * absolute target. {@link TemperatureEnum} confirms the resulting scale is
 * plain degrees Celsius: its bounds run 0-40 with {@code NORMAL} centered on
 * 20.0, the neutral point LSO assumes when no modifier is contributing.
 * Thermal Systems' {@code ambientTemperatureCelsius}, by contrast, is an
 * absolute resolved zone/ambient temperature. Passing it straight through as
 * a delta would double-count on top of LSO's own world temperature, so it is
 * converted to a delta relative to that neutral point (configurable via
 * {@link ThermalConfig#LSO_TEMPERATURE_OFFSET}, defaulting to 20.0) before
 * being handed to LSO.
 *
 * <p>{@code sourceId} (see {@link ITemperatureBridge}'s Javadoc) is passed
 * straight through as LSO's modifier UUID. Thermal Systems has two
 * independent, differently-timed callers of this method for the same player
 * - zone-ambient temperature and direct source radiation - and LSO's
 * {@code addTemperatureModifier} replaces any existing modifier registered
 * under the same UUID rather than stacking a new one. Using one shared
 * constant here regardless of caller meant whichever caller's tick ran
 * second always silently overwrote the other's contribution (observed as
 * direct-radiation deltas being immediately replaced by a zone call's 0.0
 * when no zone was bound). Forwarding each caller's own {@code sourceId}
 * instead means both land as separate modifiers on the same
 * {@code HEATING_TEMPERATURE}/{@code COOLING_TEMPERATURE} attribute, which
 * LSO already sums via {@code getAttributeValue} - no change needed on LSO's
 * read side, only here on the write side.
 */
public final class LSOThermalBridge implements ITemperatureBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSOThermalBridge.class);

    /**
     * Last delta logged per {@code sourceId} - see {@link ITemperatureBridge}'s
     * Javadoc on why each caller's contribution must be tracked independently.
     * {@code PlayerTemperatureBridgeHandler} and {@code SourceRadiationTickHandler}
     * both call this bridge every interval regardless of whether their delivered
     * value changed, so without this check every interval would log a line per
     * caller forever. The modifier is still applied on every call; only the log
     * line is gated.
     */
    private static final Map<UUID, Double> LAST_LOGGED_DELTA = new ConcurrentHashMap<>();

    @Override
    public void applyAmbientTemperature(ServerPlayer player, double ambientTemperatureCelsius, UUID sourceId) {
        double delta = ambientTemperatureCelsius - ThermalConfig.LSO_TEMPERATURE_OFFSET.get();
        if (ThermalConfig.LOGGING_ENABLED.get() && LOGGER.isDebugEnabled()) {
            Double previous = LAST_LOGGED_DELTA.put(sourceId, delta);
            if (previous == null || previous.doubleValue() != delta) {
                LOGGER.debug("[MTS] LSOThermalBridge.applyAmbientTemperature player={} ambientC={} delta={} sourceId={}",
                        player.getGameProfile().getName(), ambientTemperatureCelsius, delta, sourceId);
            }
        }
        TemperatureUtil.addTemperatureModifier(player, delta, sourceId);
    }
}
