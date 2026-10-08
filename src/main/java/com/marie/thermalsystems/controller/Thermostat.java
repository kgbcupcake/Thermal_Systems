package com.marie.thermalsystems.controller;

/**
 * Pure thermostat logic: given a zone's mode, current temperature, target and what it was doing last
 * update, decides whether the zone calls for heat, for cooling, or for nothing. Deterministic and
 * side-effect free so it can be tested without a running world.
 *
 * <p>{@link ClimateMode#HEAT} only ever heats and {@link ClimateMode#COOL} only ever cools - a
 * heat-only zone sitting above its target is left to drift back toward ambient on its own, the same
 * as a real furnace thermostat. {@link ClimateMode#AUTO} does both; {@link ClimateMode#OFF} neither.
 *
 * <p>Like a real thermostat it has hysteresis: it starts calling once the zone is more than
 * {@link #START_BAND} off target and keeps calling until it's within {@link #SATISFIED_BAND}. The
 * simulation approaches its goal exponentially and never lands exactly on it, so without the
 * satisfied band a zone would call forever (and a wall thermostat's redstone would never drop).
 *
 * <p>An idle zone drifts toward ambient and never quite reaches it, so a target closer to ambient
 * than {@link #START_BAND} would never be left far enough behind to start calling. The start band
 * therefore shrinks to half the target's distance from ambient, never below {@link #SATISFIED_BAND}.
 */
public final class Thermostat {

    public static final double START_BAND = 0.5;
    public static final double SATISFIED_BAND = 0.05;

    private Thermostat() {
    }

    public static ClimateDemand demand(ClimateMode mode, double currentTemp, double targetTemp, double ambientTemp,
                                       ClimateDemand previous) {
        boolean heatAllowed = mode == ClimateMode.HEAT || mode == ClimateMode.AUTO;
        boolean coolAllowed = mode == ClimateMode.COOL || mode == ClimateMode.AUTO;
        if (previous == ClimateDemand.HEATING && heatAllowed && currentTemp < targetTemp - SATISFIED_BAND) {
            return ClimateDemand.HEATING;
        }
        if (previous == ClimateDemand.COOLING && coolAllowed && currentTemp > targetTemp + SATISFIED_BAND) {
            return ClimateDemand.COOLING;
        }
        double startBand = startBand(targetTemp, ambientTemp);
        if (heatAllowed && currentTemp < targetTemp - startBand) {
            return ClimateDemand.HEATING;
        }
        if (coolAllowed && currentTemp > targetTemp + startBand) {
            return ClimateDemand.COOLING;
        }
        return ClimateDemand.IDLE;
    }

    static double startBand(double targetTemp, double ambientTemp) {
        double half = Math.abs(targetTemp - ambientTemp) / 2.0;
        return Math.max(SATISFIED_BAND, Math.min(START_BAND, half));
    }
}
