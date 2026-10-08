package com.marie.thermalsystems.integration.toughasnails;

import com.marie.thermalsystems.api.bridge.ITemperatureBridge;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.zone.ClimateZone;
import com.marie.thermalsystems.zone.ZoneSpatialIndex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toughasnails.api.temperature.TemperatureLevel;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges Thermal Systems' resolved per-player ambient temperature into Tough
 * As Nails. Registered from {@link ToughAsNailsIntegration#init}, itself only
 * ever called when Tough As Nails is present.
 *
 * <p>Unlike LSO and Cold Sweat, Tough As Nails has no additive modifier to
 * push a value into: it asks every registered
 * {@code IPlayerTemperatureModifier} to adjust a discrete
 * {@link TemperatureLevel} on every server tick, and the modifier can't be
 * unregistered. So {@link #applyAmbientTemperature} only records what
 * Thermal Systems last reported, and {@link #modify} - registered once with
 * Tough As Nails - turns that record into whole {@link TemperatureLevel}
 * steps on TAN's own schedule. No {@code IPositionalTemperatureModifier} is
 * registered: TAN's client thermometer calls that per frame, and zone data
 * only exists on the server.
 *
 * <p><b>Scale conversion:</b> {@code ambientTemperatureCelsius} is an
 * absolute temperature, so it's stored as a delta from
 * {@link ThermalConfig#DEFAULT_AMBIENT_TEMPERATURE} - the value both callers
 * report when nothing is heating or cooling the player, which keeps that
 * case at a delta of 0 (no steps).
 *
 * <p><b>Per-source coexistence:</b> the same as {@link
 * com.marie.thermalsystems.integration.coldsweat.ColdSweatThermalBridge}:
 * each caller's latest delta is tracked per player under its own
 * {@code sourceId} (see {@link ITemperatureBridge}'s Javadoc) and the
 * tracked deltas are summed, so the zone-ambient and direct-radiation
 * callers add together instead of one overwriting the other.
 */
public final class ToughAsNailsBridge implements ITemperatureBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToughAsNailsBridge.class);

    private final Map<UUID, Map<UUID, Double>> contributions = new ConcurrentHashMap<>();

    /**
     * Last delta logged per {@code sourceId} - see
     * {@link com.marie.thermalsystems.integration.lso.LSOThermalBridge} on why
     * only the log line, not the recorded value, is gated on a change.
     */
    private final Map<UUID, Double> lastLoggedDelta = new ConcurrentHashMap<>();

    /** What {@link #modify} last saw and returned per player - diagnostic only. */
    private final Map<UUID, LastModify> lastModify = new ConcurrentHashMap<>();

    private record LastModify(TemperatureLevel current, TemperatureLevel returned) {
    }

    @Override
    public void applyAmbientTemperature(ServerPlayer player, double ambientTemperatureCelsius, UUID sourceId) {
        double delta = ambientTemperatureCelsius - ThermalConfig.DEFAULT_AMBIENT_TEMPERATURE.get();
        contributions.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>()).put(sourceId, delta);

        if (ThermalConfig.LOGGING_ENABLED.get() && LOGGER.isDebugEnabled()) {
            Double previous = lastLoggedDelta.put(sourceId, delta);
            if (previous == null || previous.doubleValue() != delta) {
                LOGGER.debug("[MTS] ToughAsNailsBridge.applyAmbientTemperature player={} ambientC={} delta={} sourceId={} totalDelta={}",
                        player.getGameProfile().getName(), ambientTemperatureCelsius, delta, sourceId,
                        storedValue(player.getUUID()));
            }
        }
    }

    /**
     * Registered once with Tough As Nails. Player modifiers can't be
     * unregistered, so both enabled checks live here rather than only at
     * registration.
     *
     * <p>Inside a zone the room replaces TAN's outdoor environment: the
     * level comes straight from the zone's temperature, ignoring
     * {@code current}, so a heated room is warm in winter no matter how cold
     * TAN thinks the biome is. TAN applies player modifiers before items,
     * armor and food, so those still adjust the result.
     *
     * <p>Outside a zone it mirrors TAN's own armor modifier: a single source
     * never pushes a player into an extreme ({@code HOT}/{@code ICY}) from
     * outside it - that stays reserved for TAN's own environment. A player
     * already at an extreme is still adjusted normally, so opposing heat or
     * cooling can bring them back out.
     */
    public TemperatureLevel modify(Player player, TemperatureLevel current) {
        if (!ThermalConfig.SYSTEM_ENABLED.get() || !ThermalConfig.TOUGHASNAILS_ENABLED.get()) {
            return current;
        }

        Optional<ClimateZone> zone = zoneAt(player);
        if (zone.isPresent()) {
            TemperatureLevel result = TemperatureLevel.NEUTRAL.increment(
                    stepsFor(zone.get().getCurrentTemp() - ThermalConfig.DEFAULT_AMBIENT_TEMPERATURE.get()));
            lastModify.put(player.getUUID(), new LastModify(current, result));
            return result;
        }

        TemperatureLevel result = current;
        int steps = stepsFor(storedValue(player.getUUID()));
        if (steps != 0) {
            TemperatureLevel target = current.increment(steps);
            if (target == TemperatureLevel.HOT && current != TemperatureLevel.HOT) {
                result = TemperatureLevel.WARM;
            } else if (target == TemperatureLevel.ICY && current != TemperatureLevel.ICY) {
                result = TemperatureLevel.COLD;
            } else {
                result = target;
            }
        }
        lastModify.put(player.getUUID(), new LastModify(current, result));
        return result;
    }

    /** Signed step count for a summed delta in Celsius: positive heats, negative cools. */
    private static int stepsFor(double delta) {
        if (delta > 0) {
            if (delta >= ThermalConfig.TOUGHASNAILS_HEAT_TWO_STEP_THRESHOLD.get()) {
                return 2;
            }
            return delta >= ThermalConfig.TOUGHASNAILS_HEAT_ONE_STEP_THRESHOLD.get() ? 1 : 0;
        }
        if (delta < 0) {
            double cooling = -delta;
            if (cooling >= ThermalConfig.TOUGHASNAILS_COOLING_TWO_STEP_THRESHOLD.get()) {
                return -2;
            }
            return cooling >= ThermalConfig.TOUGHASNAILS_COOLING_ONE_STEP_THRESHOLD.get() ? -1 : 0;
        }
        return 0;
    }

    private static Optional<ClimateZone> zoneAt(Player player) {
        if (player.level().isClientSide()) {
            return Optional.empty();
        }
        return ZoneSpatialIndex.resolve(player.level(), player.blockPosition());
    }

    private double storedValue(UUID playerId) {
        Map<UUID, Double> perSource = contributions.get(playerId);
        if (perSource == null) {
            return 0.0;
        }
        double total = 0.0;
        for (double delta : perSource.values()) {
            total += delta;
        }
        return total;
    }

    void clearPlayer(UUID playerId) {
        contributions.remove(playerId);
        lastModify.remove(playerId);
    }

    /** One {@code /thermal debug radiation} line: stored delta, step count, TAN's level in, level returned. */
    String debugLine(ServerPlayer player) {
        double stored = storedValue(player.getUUID());
        LastModify last = lastModify.get(player.getUUID());
        Optional<ClimateZone> zone = zoneAt(player);
        return "[TAN] " + player.getGameProfile().getName()
                + (zone.isPresent() ? " zone=" + zone.get().getName() + "@" + zone.get().getCurrentTemp() + "C" : " zone=none")
                + " stored=" + stored + "C"
                + " steps=" + stepsFor(stored)
                + " current=" + (last != null ? last.current() : "n/a")
                + " returned=" + (last != null ? last.returned() : "n/a");
    }
}
