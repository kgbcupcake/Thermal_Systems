package com.marie.thermalsystems.cooling;

import com.marie.thermalsystems.api.cooling.ICoolingSource;

/**
 * Abstract base for cooling sources. Output is backed by a real field set at
 * construction rather than a constant. A CoolingSource instance is owned by
 * exactly one {@code ClimateZone} in Phase 1.
 */
public abstract class CoolingSource implements ICoolingSource {

    private final double coolingOutput;

    protected CoolingSource(double coolingOutput) {
        if (Double.isNaN(coolingOutput) || Double.isInfinite(coolingOutput)) {
            throw new IllegalArgumentException("coolingOutput must be a finite number, was: " + coolingOutput);
        }
        this.coolingOutput = coolingOutput;
    }

    @Override
    public double getCoolingOutput() {
        return coolingOutput;
    }
}
