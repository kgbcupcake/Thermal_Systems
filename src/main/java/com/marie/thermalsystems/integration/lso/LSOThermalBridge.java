package com.marie.thermalsystems.integration.lso;

import com.marie.thermalsystems.api.bridge.ITemperatureBridge;
import net.minecraft.server.level.ServerPlayer;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureEnum;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureUtil;

import java.util.UUID;

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
 * converted to a delta relative to that 20.0 neutral point before being
 * handed to LSO.
 */
public final class LSOThermalBridge implements ITemperatureBridge {

    /**
     * Fixed, unique-to-this-mod modifier id. LSO's {@code addTemperatureModifier}
     * replaces any modifier already registered under the same UUID rather than
     * stacking a new one, so reusing this constant on every call is what makes
     * repeated calls cleanly overwrite Thermal Systems' own prior contribution
     * instead of accumulating duplicates. Never regenerate this value.
     */
    private static final UUID MODIFIER_ID = UUID.fromString("0352831b-0119-47f2-8f50-5c3e9f52a6a8");

    /**
     * LSO's neutral "room temperature" point - see the class Javadoc's scale
     * check. {@code TemperatureEnum}'s {@code NORMAL} constant is centered here.
     */
    private static final double LSO_NEUTRAL_CELSIUS = 20.0;

    @Override
    public void applyAmbientTemperature(ServerPlayer player, double ambientTemperatureCelsius) {
        double delta = ambientTemperatureCelsius - LSO_NEUTRAL_CELSIUS;
        TemperatureUtil.addTemperatureModifier(player, delta, MODIFIER_ID);
    }
}
