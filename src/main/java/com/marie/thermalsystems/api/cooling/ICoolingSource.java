package com.marie.thermalsystems.api.cooling;

/**
 * Contract for a source of cooling that can be attached to a climate zone.
 */
public interface ICoolingSource {

    /**
     * Returns the current cooling output of this source, in degrees Celsius
     * per simulation second, before any coefficients are applied.
     * Implementations must compute this from real state rather than
     * returning a constant.
     */
    double getCoolingOutput();
}
