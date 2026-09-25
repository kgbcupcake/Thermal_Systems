package com.marie.thermalsystems.integration.coldsweat;

import com.marie.thermalsystems.api.bridge.ITemperatureBridge;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.momosoftworks.coldsweat.api.temperature.modifier.FrigidnessTempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.WarmthTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Matcher;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridges Thermal Systems' resolved per-player ambient temperature into Cold
 * Sweat by reusing Cold Sweat's own {@link WarmthTempModifier}/
 * {@link FrigidnessTempModifier} - its purpose-built {@code TempModifier}
 * subclasses for external heat/cold sources like its Hearth/Icebox - rather
 * than a custom subclass. Registered from {@link ColdSweatIntegration#init()},
 * itself only ever called when Cold Sweat is present.
 *
 * <p><b>Scale conversion:</b> {@code ambientTemperatureCelsius} is an
 * absolute resolved zone/ambient temperature in plain Celsius, so it's first
 * converted to a delta relative to {@link ThermalConfig#COLDSWEAT_TEMPERATURE_OFFSET}
 * (defaulting to 20.0, the same neutral point {@code defaultAmbientTemperature}
 * uses), same as the LSO bridge does. That delta cannot be forwarded
 * unscaled: {@code WarmthTempModifier}/{@code FrigidnessTempModifier} both
 * take an {@code int strength}, which {@code ThermalSourceTempModifier}
 * (their shared base) multiplies by {@code ConfigSettings.THERMAL_SOURCE_STRENGTH}
 * (0.75 by default) to get a target blended against Cold Sweat's comfortable
 * range - {@code ConfigSettings.MIN_TEMP}/{@code MAX_TEMP}, which only spans
 * about 1.2 units by default (0.5-1.7). Cold Sweat's own Hearth block - the
 * reference for what "meaningfully warm" looks like in practice - passes a
 * strength of only 0 or 1 (its insulation level, capped by
 * {@code ConfigSettings.HEARTH_MAX_INSULATION}, default 1); a strength of 1
 * already produces close to a full-range effect. {@link ThermalConfig#COLDSWEAT_OUTPUT_SCALE}
 * (default 0.1) converts the Celsius delta down into that same small-int
 * range before rounding.
 *
 * <p><b>Per-source coexistence:</b> Cold Sweat's {@link Temperature} API has
 * no UUID-keyed modifier concept the way LSO's {@code TemperatureUtil} does,
 * so {@code sourceId} (see {@link ITemperatureBridge}'s Javadoc) can't be
 * forwarded as a modifier key directly. Instead, each caller's most recent
 * contribution is tracked here, per player, keyed by its own {@code sourceId}
 * - the same principle as LSO's fixed-UUID-per-caller fix, just kept
 * bridge-side instead of pushed into Cold Sweat. Every call recomputes the
 * sum of all tracked contributions for that player and writes it into a
 * single, class-deduplicated {@code WarmthTempModifier} and
 * {@code FrigidnessTempModifier} pair (added with
 * {@code Placement.LAST.noDuplicates(Matcher.SAME_CLASS)} on first
 * contribution, mutated in place afterward), so the zone-ambient and
 * direct-radiation callers add together instead of one overwriting the
 * other.
 */
public final class ColdSweatThermalBridge implements ITemperatureBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColdSweatThermalBridge.class);

    private static final Map<UUID, Map<UUID, int[]>> CONTRIBUTIONS = new ConcurrentHashMap<>();

    /**
     * Last delta logged per {@code sourceId} - see {@link ITemperatureBridge}'s
     * Javadoc on why each caller's contribution must be tracked independently.
     * {@code PlayerTemperatureBridgeHandler} and {@code SourceRadiationTickHandler}
     * both call this bridge every interval regardless of whether their delivered
     * value changed, so without this check every interval would log a line per
     * caller forever. The modifiers are still applied on every call; only the
     * log line is gated.
     */
    private static final Map<UUID, Double> LAST_LOGGED_DELTA = new ConcurrentHashMap<>();

    @Override
    public void applyAmbientTemperature(ServerPlayer player, double ambientTemperatureCelsius, UUID sourceId) {
        double delta = ambientTemperatureCelsius - ThermalConfig.COLDSWEAT_TEMPERATURE_OFFSET.get();
        int scaled = scaleDelta(delta);
        // WarmthTempModifier/FrigidnessTempModifier both store their strength as an
        // always-non-negative int (Cold Sweat keeps "Warming"/"Cooling" as two separate
        // NBT ints, never a signed one), so the sign of scaled selects which one is
        // active and Math.abs() extracts its magnitude explicitly - never hand a
        // negative int to either constructor.
        int warming = scaled > 0 ? scaled : 0;
        int cooling = scaled < 0 ? Math.abs(scaled) : 0;

        Map<UUID, int[]> contributions = CONTRIBUTIONS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
        contributions.put(sourceId, new int[] {warming, cooling});

        int totalWarming = 0;
        int totalCooling = 0;
        for (int[] contribution : contributions.values()) {
            totalWarming += contribution[0];
            totalCooling += contribution[1];
        }

        if (ThermalConfig.LOGGING_ENABLED.get()) {
            Double previous = LAST_LOGGED_DELTA.put(sourceId, delta);
            if (previous == null || previous.doubleValue() != delta) {
                LOGGER.info("[MTS] ColdSweatThermalBridge.applyAmbientTemperature player={} ambientC={} delta={} sourceId={} totalWarming={} totalCooling={}",
                        player.getGameProfile().getName(), ambientTemperatureCelsius, delta, sourceId, totalWarming, totalCooling);
            }
        }

        applyWarmth(player, totalWarming);
        applyFrigidness(player, totalCooling);
    }

    static void clearPlayer(UUID playerId) {
        CONTRIBUTIONS.remove(playerId);
    }

    /**
     * {@code Math.round(delta * outputScale)} alone lets any active heat/cooling
     * source go completely invisible to Cold Sweat: with the default 0.1 scale,
     * every delta under 5C rounds to strength 0 and silently produces no
     * modifier at all, even though the source is genuinely contributing. Below
     * a tiny epsilon (float noise, not a real signal) delta is treated as
     * exactly zero; above it, the scaled magnitude is floored at 1 so a real,
     * nonzero contribution is never discarded outright - it only ever gets
     * scaled up from the weakest possible effect, never down to nothing.
     */
    private static int scaleDelta(double delta) {
        if (Math.abs(delta) < 0.01) {
            return 0;
        }
        double magnitude = Math.abs(delta) * ThermalConfig.COLDSWEAT_OUTPUT_SCALE.get();
        int strength = Math.max(1, (int) Math.round(magnitude));
        return delta > 0 ? strength : -strength;
    }

    private static final AtomicBoolean WARMTH_WARNED_ONCE = new AtomicBoolean(false);
    private static final AtomicBoolean FRIGIDNESS_WARNED_ONCE = new AtomicBoolean(false);

    private static void applyWarmth(ServerPlayer player, int warming) {
        try {
            Temperature.replaceOrAddModifier(player, new WarmthTempModifier(warming), Temperature.Trait.WORLD, Matcher.SAME_CLASS);
        } catch (Exception e) {
            if (ThermalConfig.LOGGING_ENABLED.get() || WARMTH_WARNED_ONCE.compareAndSet(false, true)) {
                LOGGER.warn("[MTS] ColdSweatThermalBridge.applyWarmth caught exception from Cold Sweat for player={}",
                        player.getGameProfile().getName(), e);
            }
        }
    }

    private static void applyFrigidness(ServerPlayer player, int cooling) {
        try {
            Temperature.replaceOrAddModifier(player, new FrigidnessTempModifier(cooling), Temperature.Trait.WORLD, Matcher.SAME_CLASS);
        } catch (Exception e) {
            if (ThermalConfig.LOGGING_ENABLED.get() || FRIGIDNESS_WARNED_ONCE.compareAndSet(false, true)) {
                LOGGER.warn("[MTS] ColdSweatThermalBridge.applyFrigidness caught exception from Cold Sweat for player={}",
                        player.getGameProfile().getName(), e);
            }
        }
    }
}
