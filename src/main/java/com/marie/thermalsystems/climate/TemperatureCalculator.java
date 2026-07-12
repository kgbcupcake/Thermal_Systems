package com.marie.thermalsystems.climate;

import com.marie.thermalsystems.api.climate.ITemperatureCalculator;

/**
 * Computes a zone's next temperature. Deterministic and side-effect free;
 * every coefficient, rate, or clamp value is a parameter, never a literal
 * or a config read.
 */
public class TemperatureCalculator implements ITemperatureCalculator {

    @Override
    public double computeNextTemperature(
            double currentTemperature,
            double targetTemperature,
            double totalHeatOutput,
            double deltaTime,
            double temperatureConvergenceRate,
            double heatTransferCoefficient,
            double minimumTemperature,
            double maximumTemperature
    ) {
        double effectiveConvergenceRate = temperatureConvergenceRate + (heatTransferCoefficient * totalHeatOutput);

        double nextTemperature = currentTemperature +
                (targetTemperature - currentTemperature) *
                        (1.0 - Math.exp(-effectiveConvergenceRate * deltaTime));

        return clamp(nextTemperature, minimumTemperature, maximumTemperature);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
