package com.marie.thermalsystems.api.climate;

/**
 * Pure contract for computing a zone's next temperature given its current
 * state, the combined output of its active heat/cool sources, and the
 * elapsed simulation time.
 */
public interface ITemperatureCalculator {

    /**
     * Computes the next temperature for a zone.
     *
     * @param currentTemperature        the zone's current temperature, in Celsius
     * @param targetTemperature         the zone's target temperature, in Celsius
     * @param totalHeatOutput           the summed output of all active heat sources
     *                                   (and cooling sources, once implemented), in
     *                                   degrees Celsius per simulation second
     * @param deltaTime                 elapsed simulation time, in seconds
     * @param temperatureConvergenceRate the zone's baseline drift-to-target rate
     * @param heatTransferCoefficient   scales how much heat output accelerates convergence
     * @param minimumTemperature        clamp floor, in Celsius
     * @param maximumTemperature        clamp ceiling, in Celsius
     * @return the next temperature, clamped to [minimumTemperature, maximumTemperature]
     */
    double computeNextTemperature(
            double currentTemperature,
            double targetTemperature,
            double totalHeatOutput,
            double deltaTime,
            double temperatureConvergenceRate,
            double heatTransferCoefficient,
            double minimumTemperature,
            double maximumTemperature
    );
}
