package com.marie.thermalsystems.integration.coldsweat;

import com.marie.thermalsystems.api.bridge.ITemperatureBridge;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.momosoftworks.coldsweat.api.temperature.modifier.SimpleTempModifier;
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
 * Sweat. Registered from {@link ColdSweatIntegration#init()}, itself only
 * ever called when Cold Sweat is present.
 *
 * <p><b>Why {@link SimpleTempModifier}, not {@code WarmthTempModifier}/
 * {@code FrigidnessTempModifier}:</b> those are Cold Sweat's own Hearth/Icebox
 * modifiers, and {@code ThermalSourceTempModifier.calculate()} only ever
 * nudges the player's temperature back toward Cold Sweat's comfortable
 * midpoint - warming only applies while already below it, cooling only while
 * already above it. Neither can push a player past that midpoint into Hot or
 * Cold, which is exactly what a real heat/cooling source needs to do.
 * {@link SimpleTempModifier} with {@link SimpleTempModifier.Operation#ADD}
 * applies an unconditional {@code temp + value}, so a source works in both
 * directions regardless of the player's current temperature.
 *
 * <p><b>Scale conversion:</b> {@code ambientTemperatureCelsius} is converted
 * to a delta relative to {@link ThermalConfig#COLDSWEAT_TEMPERATURE_OFFSET}
 * (defaulting to 20.0, the same neutral point {@code defaultAmbientTemperature}
 * uses) and multiplied by {@link ThermalConfig#COLDSWEAT_OUTPUT_SCALE}
 * (default 0.1) to convert it into Cold Sweat's own small-magnitude WORLD
 * temperature units.
 *
 * <p><b>Per-source coexistence:</b> Cold Sweat's {@link Temperature} API has
 * no UUID-keyed modifier concept, so {@code sourceId} (see
 * {@link ITemperatureBridge}'s Javadoc) can't be forwarded as a modifier key
 * directly. Instead, each caller's most recent contribution is tracked here,
 * per player, keyed by its own {@code sourceId}. Every call recomputes the
 * sum of all tracked contributions and writes it into a single,
 * class-deduplicated {@link SimpleTempModifier} via
 * {@link Temperature#replaceOrAddModifier}, which updates that modifier in
 * place rather than refusing to touch it once one already exists (unlike
 * {@code Placement.LAST.noDuplicates(...)}, whose {@code maxDuplicates}
 * gate only lets the *first* call through).
 */
public final class ColdSweatThermalBridge implements ITemperatureBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColdSweatThermalBridge.class);

    private static final Map<UUID, Map<UUID, Double>> CONTRIBUTIONS = new ConcurrentHashMap<>();

    /**
     * Last delta logged per {@code sourceId} - see {@link ITemperatureBridge}'s
     * Javadoc on why each caller's contribution must be tracked independently.
     * {@code PlayerTemperatureBridgeHandler} and {@code SourceRadiationTickHandler}
     * both call this bridge every interval regardless of whether their delivered
     * value changed, so without this check every interval would log a line per
     * caller forever. The modifier is still applied on every call; only the
     * log line is gated.
     */
    private static final Map<UUID, Double> LAST_LOGGED_DELTA = new ConcurrentHashMap<>();

    @Override
    public void applyAmbientTemperature(ServerPlayer player, double ambientTemperatureCelsius, UUID sourceId) {
        double delta = ambientTemperatureCelsius - ThermalConfig.COLDSWEAT_TEMPERATURE_OFFSET.get();
        double scaled = delta * ThermalConfig.COLDSWEAT_OUTPUT_SCALE.get();

        Map<UUID, Double> contributions = CONTRIBUTIONS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
        contributions.put(sourceId, scaled);

        double total = 0.0;
        for (double contribution : contributions.values()) {
            total += contribution;
        }

        if (ThermalConfig.LOGGING_ENABLED.get()) {
            Double previous = LAST_LOGGED_DELTA.put(sourceId, delta);
            if (previous == null || previous.doubleValue() != delta) {
                LOGGER.info("[MTS] ColdSweatThermalBridge.applyAmbientTemperature player={} ambientC={} delta={} sourceId={} total={}",
                        player.getGameProfile().getName(), ambientTemperatureCelsius, delta, sourceId, total);
            }
        }

        applyTemperature(player, total);
    }

    static void clearPlayer(UUID playerId) {
        CONTRIBUTIONS.remove(playerId);
    }

    private static final AtomicBoolean APPLY_WARNED_ONCE = new AtomicBoolean(false);

    private static void applyTemperature(ServerPlayer player, double total) {
        try {
            Temperature.replaceOrAddModifier(player, new SimpleTempModifier(total, SimpleTempModifier.Operation.ADD),
                    Temperature.Trait.WORLD, Matcher.SAME_CLASS);
        } catch (Exception e) {
            if (ThermalConfig.LOGGING_ENABLED.get() || APPLY_WARNED_ONCE.compareAndSet(false, true)) {
                LOGGER.warn("[MTS] ColdSweatThermalBridge.applyTemperature caught exception from Cold Sweat for player={}",
                        player.getGameProfile().getName(), e);
            }
        }
    }
}
