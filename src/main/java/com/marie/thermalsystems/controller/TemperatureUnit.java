package com.marie.thermalsystems.controller;

import java.util.Locale;

/**
 * How temperatures are shown to a player. Zones are simulated, stored and configured in Celsius
 * throughout; a unit only changes what a player sees and how far a thermostat's +/- buttons step.
 */
public enum TemperatureUnit {

    CELSIUS("\u00B0C", 0.5, 5.0),
    FAHRENHEIT("\u00B0F", 1.0, 10.0);

    /** Slack, in steps, so a converted value sitting a rounding error off the grid still counts as on it. */
    private static final double GRID_EPSILON = 1e-6;

    private final String symbol;
    private final double step;
    private final double largeStep;

    TemperatureUnit(String symbol, double step, double largeStep) {
        this.symbol = symbol;
        this.step = step;
        this.largeStep = largeStep;
    }

    public String symbol() {
        return symbol;
    }

    public TemperatureUnit next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public double fromCelsius(double celsius) {
        return this == FAHRENHEIT ? celsius * 9.0 / 5.0 + 32.0 : celsius;
    }

    public double toCelsius(double value) {
        return this == FAHRENHEIT ? (value - 32.0) * 5.0 / 9.0 : value;
    }

    /** Converts a temperature difference (no zero-point offset). */
    public double deltaFromCelsius(double celsius) {
        return this == FAHRENHEIT ? celsius * 9.0 / 5.0 : celsius;
    }

    public double deltaToCelsius(double value) {
        return this == FAHRENHEIT ? value * 5.0 / 9.0 : value;
    }

    /** {@code celsius} in this unit with one decimal and its symbol, e.g. {@code 73.4°F}. */
    public String format(double celsius) {
        return String.format(Locale.ROOT, "%.1f%s", fromCelsius(celsius), symbol);
    }

    /**
     * The target one +/- press ({@code direction} positive or negative) away from
     * {@code targetCelsius}, back in Celsius. The result lands on this unit's step grid, so a
     * Fahrenheit thermostat reads 73, 74, 75 rather than converted half-degree Celsius steps.
     */
    public double step(double targetCelsius, int direction, boolean large) {
        double value = fromCelsius(targetCelsius) / step;
        double size = (large ? largeStep : step) / step;
        double next = direction > 0
                ? Math.floor(value + GRID_EPSILON) + size
                : Math.ceil(value - GRID_EPSILON) - size;
        return toCelsius(next * step);
    }
}
