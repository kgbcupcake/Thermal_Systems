package com.marie.thermalsystems.heating;

import com.marie.thermalsystems.api.heating.IHeatSource;

/**
 * Abstract base for heat sources. Output is backed by a real field set at
 * construction rather than a constant. A HeatSource instance is owned by
 * exactly one {@code ClimateZone} in Phase 1.
 */
public abstract class HeatSource implements IHeatSource {

    private final double heatOutput;

    protected HeatSource(double heatOutput) {
        if (Double.isNaN(heatOutput) || Double.isInfinite(heatOutput)) {
            throw new IllegalArgumentException("heatOutput must be a finite number, was: " + heatOutput);
        }
        this.heatOutput = heatOutput;
    }

    @Override
    public double getHeatOutput() {
        return heatOutput;
    }
}
