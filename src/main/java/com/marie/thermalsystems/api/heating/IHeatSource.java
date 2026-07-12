package com.marie.thermalsystems.api.heating;

/**
 * Contract for a source of heat that can be attached to a climate zone.
 */
public interface IHeatSource {

    /**
     * Returns the current heat output of this source, in degrees Celsius per
     * simulation second, before any coefficients are applied. Implementations
     * must compute this from real state rather than returning a constant.
     */
    double getHeatOutput();
}
